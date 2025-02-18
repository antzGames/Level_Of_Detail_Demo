package com.antz.lod;

import com.antz.lod.gdxgltf.LoDScene;
import com.antz.lod.gdxgltf.LoDSceneManager;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import text.formic.Stringf;

public class Main extends ApplicationAdapter {
    private LoDSceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Array<LoDScene> scenes = new Array<>();
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private FirstPersonCameraController controller;
    private SpriteBatch spriteBatch;
    private BitmapFont bitmapFont;
    private int totalIndices = 0;
    private Vector3 tempVec = new Vector3();

    // *** You can change Model
    private String modelPrefix = "models/bunny/Bunny";
//    private String modelPrefix = "models/arma/Armadillo";
//    private String modelPrefix = "models/Dragon/Dragon";

    @Override
    public void create() {
        loadFiles();
        configEnvironment();
        setupCamera();
        setupGUI();
    }

    private void loadFiles() {
        // create scene
        sceneManager = new LoDSceneManager(0);

        boolean loaded = false;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                // Creating a new sceneAsset in the loop mimics 100 DIFFERENT GLTF files being loaded
                sceneAsset = new GLTFLoader().load(Gdx.files.internal(modelPrefix + ".gltf"));
                LoDScene scene = new LoDScene(sceneAsset.scene);

                scene.modelInstance.transform.translate(i* 10 - 50, 0, j * 10 - 50);

                // The bunny is a bit too small, lets make it bigger
                if (modelPrefix.endsWith("Bunny")) scene.modelInstance.transform.scl(2.5f);

                scene.modelInstance.transform.rotate(Vector3.Y, MathUtils.random(360));

                // This next part reduces the times the LoD meta data file is loaded to 1
                if (loaded) {
                    scene.copyLoDs(scenes.first()); // helper method to copy LoD meta data between duplicate scenes.
                } else {
                    // You can call this for every duplicated instance, but it will be slower.
                    scene.loadLoDs(Gdx.files.internal(modelPrefix + ".lod")); // NEW! Load the LoD meta data
                    loaded = true;
                }
                scenes.add(scene);
                sceneManager.addScene(scene);
            }
        }
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        checkInput();

        // camera
        controller.update();
        camera.up.set(Vector3.Y);
        camera.update();

        // Rotate models
        totalIndices = 0;
        for (LoDScene s: scenes) {
            s.modelInstance.transform.rotate(Vector3.Y, Gdx.graphics.getDeltaTime() * 45f);
            totalIndices += s.currentIndices;
        }

        // gdx-gltf update + render
        ScreenUtils.clear(Color.RED, true);
        sceneManager.update(deltaTime);
        sceneManager.updateLoDs(); // NEW!!! check if LoDs need updating
        sceneManager.render();

        // labels
        updateLabels();
    }

    private void updateLabels() {
        spriteBatch.begin();
        if (scenes.first().lodEnabled) {
            bitmapFont.setColor(Color.WHITE);
            bitmapFont.draw(spriteBatch, "Total LoD indices: " + totalIndices, 10, 75);

            int x = 10; int y = Gdx.graphics.getHeight() - 10; int count = 0;
            for (LoDScene s: scenes) {
                switch (s.currentLoDLevel){
                    case 0: bitmapFont.setColor(Color.RED); break;
                    case 1: bitmapFont.setColor(Color.ORANGE); break;
                    case 2: bitmapFont.setColor(Color.GOLD); break;
                    case 3: bitmapFont.setColor(Color.YELLOW); break;
                    case 4: bitmapFont.setColor(Color.GREEN); break;
                    case 5: bitmapFont.setColor(Color.TEAL); break;
                    case 6: bitmapFont.setColor(Color.BLUE); break;
                    case 7: bitmapFont.setColor(Color.VIOLET); break;
                }
                bitmapFont.draw(spriteBatch, "LoD"+ s.currentLoDLevel , x, y);
                x += 42; count++;
                if (count >= 10) {
                    x = 10; y -= 25; count = 0;
                }
            }
        } else {
            bitmapFont.setColor(Color.RED);
            bitmapFont.draw(spriteBatch, "Total indices: " + totalIndices, 10, 75);
        }

        bitmapFont.setColor(Color.WHITE);
        bitmapFont.draw(spriteBatch,"Camera: " + Stringf.format("%.1f, %.1f, %.1f", camera.position.x, camera.position.y, camera.position.z),10,50);
        bitmapFont.draw(spriteBatch,"FPS: " + Gdx.graphics.getFramesPerSecond(),10,25);
        bitmapFont.draw(spriteBatch,"L Key: Toggles LoD", Gdx.graphics.getWidth() - 150, 50);
        bitmapFont.draw(spriteBatch,"R Key: Reset Camera", Gdx.graphics.getWidth() - 150, 75);

        bitmapFont.draw(spriteBatch, "Mouse/WASD for Camera", Gdx.graphics.getWidth() - 175, 100);
        if (scenes.first().lodEnabled) {
            bitmapFont.setColor(Color.GREEN);
            bitmapFont.draw(spriteBatch, "LoD currently: ON", Gdx.graphics.getWidth() - 150, 25);
        } else {
            bitmapFont.setColor(Color.RED);
            bitmapFont.draw(spriteBatch, "LoD currently: OFF", Gdx.graphics.getWidth() - 150, 25);
        }
        spriteBatch.end();
    }

    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !Gdx.app.getType().equals(Application.ApplicationType.WebGL)) Gdx.app.exit();
        else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            camera.position.set(53f, 32.7f, -53f);
            camera.lookAt(tempVec.set(20,1,-30));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.L) && scenes.first().lodSupported) {
            for (LoDScene s: scenes){
                if (s.lodEnabled) {
                    s.lodEnabled = false;
                    s.changeModelToLoDLevel(s.modelInstance.model, 0);
                } else {
                    s.lodEnabled = true;
                  }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    private void configEnvironment() {
        // setup light
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.2f;
        camera.far = 1000f;
        camera.position.set(53f, 32.7f, -53f);
        camera.lookAt(tempVec.set(20,1,-30));
        sceneManager.setCamera(camera);

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(12);
        controller.setDegreesPerPixel(0.05f);
        Gdx.input.setInputProcessor(controller);
    }

    private void setupGUI() {
        spriteBatch = new SpriteBatch();
        bitmapFont = new BitmapFont();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        spriteBatch.dispose();
        bitmapFont.dispose();
    }
}
