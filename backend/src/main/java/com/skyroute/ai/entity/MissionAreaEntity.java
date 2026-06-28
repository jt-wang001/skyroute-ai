package com.skyroute.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("mission_area")
public class MissionAreaEntity extends BaseEntity {

    @TableField("mission_id")
    private Long missionId;

    @TableField("area_geojson")
    private String areaGeojson;

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public String getAreaGeojson() {
        return areaGeojson;
    }

    public void setAreaGeojson(String areaGeojson) {
        this.areaGeojson = areaGeojson;
    }
}
