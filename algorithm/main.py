from math import asin, cos, radians, sin, sqrt
from typing import Any, Literal

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, field_validator


EARTH_RADIUS_METERS = 6_371_000
BASE_SWATH_WIDTH_METERS = 30
MIN_LINE_SPACING_METERS = 3
MAX_WAYPOINTS = 500


class GeoJsonPolygon(BaseModel):
    type: Literal["Polygon"]
    coordinates: list[list[list[float]]]

    @field_validator("coordinates", mode="before")
    @classmethod
    def normalize_coordinates(cls, coordinates: Any) -> Any:
        # Standard GeoJSON Polygon: [[[lng, lat], ...]]
        # Also accept [[lng, lat], ...] and wrap it as one outer ring.
        if coordinates and isinstance(coordinates, list):
            first_item = coordinates[0]
            if (
                isinstance(first_item, list)
                and len(first_item) >= 2
                and all(isinstance(value, (int, float)) for value in first_item[:2])
            ):
                return [coordinates]
        return coordinates

    @field_validator("coordinates")
    @classmethod
    def validate_coordinates(
        cls, coordinates: list[list[list[float]]]
    ) -> list[list[list[float]]]:
        if not coordinates:
            raise ValueError("areaGeojson.coordinates is required")

        outer_ring = coordinates[0]
        if len(outer_ring) < 4:
            raise ValueError("Polygon outer ring needs at least 4 coordinates and must be closed")

        if outer_ring[0] != outer_ring[-1]:
            raise ValueError("Polygon outer ring must be closed: first coordinate must equal last coordinate")

        for coordinate in outer_ring:
            if len(coordinate) < 2:
                raise ValueError("Each coordinate must contain longitude and latitude")

            longitude = coordinate[0]
            latitude = coordinate[1]
            if longitude < -180 or longitude > 180:
                raise ValueError("Longitude must be between -180 and 180")
            if latitude < -90 or latitude > 90:
                raise ValueError("Latitude must be between -90 and 90")

        distinct_points = {tuple(point[:2]) for point in outer_ring[:-1]}
        if len(distinct_points) < 3:
            raise ValueError("Polygon outer ring needs at least 3 distinct coordinates")

        return coordinates


class GenerateRouteRequest(BaseModel):
    areaGeojson: GeoJsonPolygon
    flightHeight: float = Field(gt=0, le=500, description="Flight height, meters")
    flightSpeed: float = Field(gt=0, le=30, description="Flight speed, m/s")
    sideOverlap: float = Field(ge=0, le=100, description="Side overlap rate, percent")


class Waypoint(BaseModel):
    index: int
    longitude: float
    latitude: float
    altitude: float
    # Keep these two fields for current Spring Boot backend compatibility.
    sequence: int
    actionType: str


class GenerateRouteResponse(BaseModel):
    success: bool
    message: str
    distanceMeters: float
    estimatedTimeSeconds: float
    waypointCount: int
    waypoints: list[Waypoint]


class HealthResponse(BaseModel):
    status: str
    service: str


app = FastAPI(
    title="SkyRoute AI Algorithm Service",
    description="SkyRoute AI route planning algorithm service",
    version="0.2.1",
)


def haversine_distance_meters(
    longitude1: float,
    latitude1: float,
    longitude2: float,
    latitude2: float,
) -> float:
    lon1 = radians(longitude1)
    lat1 = radians(latitude1)
    lon2 = radians(longitude2)
    lat2 = radians(latitude2)

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * asin(sqrt(a))
    return EARTH_RADIUS_METERS * c


def meters_to_latitude_delta(meters: float) -> float:
    return meters / 111_320


def build_lawn_mower_waypoints(request: GenerateRouteRequest) -> list[Waypoint]:
    outer_ring = request.areaGeojson.coordinates[0][:-1]
    longitudes = [point[0] for point in outer_ring]
    latitudes = [point[1] for point in outer_ring]

    min_longitude = min(longitudes)
    max_longitude = max(longitudes)
    min_latitude = min(latitudes)
    max_latitude = max(latitudes)

    width_meters = haversine_distance_meters(
        min_longitude, min_latitude, max_longitude, min_latitude
    )
    height_meters = haversine_distance_meters(
        min_longitude, min_latitude, min_longitude, max_latitude
    )

    if width_meters <= 0 or height_meters <= 0:
        raise ValueError("Polygon boundary is invalid; route cannot be generated")

    line_spacing_meters = max(
        BASE_SWATH_WIDTH_METERS * (1 - request.sideOverlap / 100),
        MIN_LINE_SPACING_METERS,
    )
    latitude_step = meters_to_latitude_delta(line_spacing_meters)

    waypoints: list[Waypoint] = []
    current_latitude = min_latitude
    line_index = 0

    while current_latitude <= max_latitude:
        if len(waypoints) + 2 > MAX_WAYPOINTS:
            raise ValueError(
                f"Generated waypoint count exceeds {MAX_WAYPOINTS}; reduce area size or side overlap"
            )

        start_longitude, end_longitude = (
            (min_longitude, max_longitude)
            if line_index % 2 == 0
            else (max_longitude, min_longitude)
        )

        for longitude in (start_longitude, end_longitude):
            index = len(waypoints) + 1
            waypoints.append(
                Waypoint(
                    index=index,
                    sequence=index,
                    longitude=round(longitude, 8),
                    latitude=round(current_latitude, 8),
                    altitude=request.flightHeight,
                    actionType="FLY",
                )
            )

        current_latitude += latitude_step
        line_index += 1

    if waypoints and waypoints[-1].latitude < max_latitude:
        start_longitude, end_longitude = (
            (min_longitude, max_longitude)
            if line_index % 2 == 0
            else (max_longitude, min_longitude)
        )
        for longitude in (start_longitude, end_longitude):
            index = len(waypoints) + 1
            waypoints.append(
                Waypoint(
                    index=index,
                    sequence=index,
                    longitude=round(longitude, 8),
                    latitude=round(max_latitude, 8),
                    altitude=request.flightHeight,
                    actionType="FLY",
                )
            )

    waypoints[0].actionType = "TAKEOFF"
    waypoints[-1].actionType = "LAND"
    return waypoints


def calculate_total_distance_meters(waypoints: list[Waypoint]) -> float:
    if len(waypoints) < 2:
        return 0

    total_distance = 0.0
    for previous, current in zip(waypoints, waypoints[1:]):
        total_distance += haversine_distance_meters(
            previous.longitude,
            previous.latitude,
            current.longitude,
            current.latitude,
        )
    return total_distance


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    raw_body = (await request.body()).decode("utf-8", errors="replace")
    errors = [
        {
            "field": ".".join(str(part) for part in error.get("loc", [])),
            "message": error.get("msg", "Invalid request parameter"),
        }
        for error in exc.errors()
    ]
    print("[generate-route validation failed]", {"errors": errors, "body": raw_body})
    return JSONResponse(
        status_code=400,
        content={
            "detail": "Request validation failed",
            "errors": errors,
        },
    )


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="UP", service="skyroute-ai-algorithm")


@app.post("/generate-route", response_model=GenerateRouteResponse)
async def generate_route(request: GenerateRouteRequest) -> GenerateRouteResponse:
    try:
        waypoints = build_lawn_mower_waypoints(request)
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error

    distance_meters = calculate_total_distance_meters(waypoints)
    estimated_time_seconds = distance_meters / request.flightSpeed

    return GenerateRouteResponse(
        success=True,
        message="Lawn mower route generated successfully",
        distanceMeters=round(distance_meters, 2),
        estimatedTimeSeconds=round(estimated_time_seconds, 2),
        waypointCount=len(waypoints),
        waypoints=waypoints,
    )