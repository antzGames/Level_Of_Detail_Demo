package com.antz.lod.gdxgltf;

import com.antz.lod.dto.LevelOfDetailDTO;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.utils.Json;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneModel;

import java.util.Arrays;

public class LoDScene extends Scene {

    public LevelOfDetailDTO levelOfDetailDTO;
    public boolean lodEnabled = false;

    public int currentLoDLevel = 0;
    public int numberOfLoDs;
    public int numberOfMeshes;

    // stats
    public int currentVertices;
    public int currentIndices;

    private int numberOfMeshesTemp = 0;
    private static final Json json = new Json();

    public LoDScene(ModelInstance modelInstance) {
        super(modelInstance);
    }

    public LoDScene(SceneModel sceneModel) {
        super(sceneModel);
    }

    public void loadLoDs(FileHandle file){
        // If no file found or any deserialization issues, revert back to normal gdx-gltf functionality
        try {
            levelOfDetailDTO = json.fromJson(LevelOfDetailDTO.class, file.readString());
        } catch (Exception e) {
            Gdx.app.error("LOD", "File: " + file.name() + " LoD meta data failed to load.  LoD subsystem disabled.");
            lodEnabled = false;
            return;
        }

        Gdx.app.log("LOD","File: " + file.name() + " has been imported.  # of meshes: " + levelOfDetailDTO.meshes.size + "    # of LoDs per mesh: " + levelOfDetailDTO.lodDistanceBreakpoints.length);
        Gdx.app.log("LOD", "Camera distance breakpoints set to: " + Arrays.toString(levelOfDetailDTO.lodDistanceBreakpoints));
        lodEnabled = true;
        numberOfLoDs = levelOfDetailDTO.lodDistanceBreakpoints.length;
        numberOfMeshes = levelOfDetailDTO.meshes.size;

        changeModelToLoDLevel(modelInstance.model,0); // reset to original detail on (re)load of LoD meta data
    }

    public void changeModelToLoDLevel(Model model, int lodLevel){
        // Simple checks on lodLevel
        if (lodLevel < 0) lodLevel = 0;
        if (lodLevel > (numberOfLoDs - 1)) lodLevel = numberOfLoDs - 1;

        numberOfMeshesTemp = 0;
        for (Node node: model.nodes){
            doLoDSwap(node, lodLevel);
            doChildrenLoDSwap(node.getChildren(), lodLevel);
        }
    }

    private void doLoDSwap(Node node, int level) {
        currentVertices = 0; currentIndices = 0;
        if (node.parts.size > 0) {
            numberOfMeshesTemp++;

            node.parts.first().meshPart.mesh.setIndices(
                levelOfDetailDTO.meshes.get(numberOfMeshesTemp - 1).lods.get(level).indicies);
            node.parts.first().meshPart.size = levelOfDetailDTO.meshes.get(numberOfMeshesTemp - 1).lods.get(level).indicies.length;

            currentVertices += node.parts.first().meshPart.mesh.getNumVertices();
            currentIndices += node.parts.first().meshPart.mesh.getNumIndices();
        }
    }

    private void doChildrenLoDSwap(Iterable<Node> children, int level) {
        for (Node node: children) {
            doLoDSwap(node, level);
            doChildrenLoDSwap(node.getChildren(), level);
        }
    }
}
