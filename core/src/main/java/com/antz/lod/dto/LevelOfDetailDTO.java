package com.antz.lod.dto;

import com.badlogic.gdx.utils.Array;

public class LevelOfDetailDTO {
        public float[] lodDistanceBreakpoints = new float[8];
        public Array<MeshLoDDTO> meshes = new Array<>();
}
