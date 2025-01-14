package com.antz.lod.gdxgltf;

import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class LoDSceneManager extends SceneManager {

    public LoDSceneManager(int maxBones) {
        super(maxBones);
    }

    private float cameraDistance;
    private Vector3 pos = new Vector3();

    public void updateLoDs(){
        if (camera != null){
            for (RenderableProvider r : getRenderableProviders()){
                for (int i = 0; i < ((LoDScene)r).numberOfLoDs ; i++){
                    if (!((LoDScene)r).lodEnabled) continue;

                    // get camera distance from scene's modelInstance (TODO: should be center of Scene's BB)
                    ((LoDScene)r).modelInstance.transform.getTranslation(pos);
                    cameraDistance = camera.position.dst(pos);

                    if (cameraDistance < ((LoDScene)r).levelOfDetailDTO.lodDistanceBreakpoints[i]) {
                        if (((LoDScene)r).currentLoDLevel != i) {
                            ((LoDScene)r).changeModelToLoDLevel(((LoDScene)r).modelInstance.model, i);
                            ((LoDScene)r).currentLoDLevel = i;
                        }

                        break;
                    }
                }
            }
        }
    }
}
