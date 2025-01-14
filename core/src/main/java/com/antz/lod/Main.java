package com.antz.lod;

import com.antz.lod.gdxgltf.LoDScene;
import com.antz.lod.gdxgltf.LoDSceneManager;
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
    private String modelPrefix = "models/Dragon/Dragon";
    //private String modelPrefix = "models/Bunny/Bunny";

    @Override
    public void create() {

        /*
         * Swap comments between with LoDs or without
         * to see the FPS difference on your machine!
         *
         * Pick only one
         */

        loadFilesWithLoDs();  // LoD enabled - super charge libGDX
        // *** OR ***
        //loadFiles();        // regular slow and inefficient libGDX

        configEnvironment();
        setupCamera();
        setupGUI();
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

        // update + render
        ScreenUtils.clear(Color.RED, true);
        sceneManager.update(deltaTime);
        sceneManager.updateLoDs(); // NEW!!! check if LoDs need updating
        sceneManager.render();

        // labels
        spriteBatch.begin();
        if (scenes.first().lodEnabled)
            bitmapFont.draw(spriteBatch,"Total LoD indices: " + totalIndices,20,150);
        bitmapFont.draw(spriteBatch,"Camera: " + Stringf.format("%.1f, %.1f, %.1f", camera.position.x, camera.position.y, camera.position.z),20,100);
        bitmapFont.draw(spriteBatch,"FPS: " + Gdx.graphics.getFramesPerSecond(),20,50);
        spriteBatch.end();
    }

    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    private void loadFiles() {
        // create scene
        sceneManager = new LoDSceneManager(0);

        // Since meshes are not changing do to LoDs, we can reuse the same SceneAsset
        sceneAsset = new GLTFLoader().load(Gdx.files.internal(modelPrefix + ".gltf"));

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                LoDScene s = new LoDScene(sceneAsset.scene);
                s.modelInstance.transform.translate(i* 10 - 50, 0, j * 10 - 50);
                s.modelInstance.transform.rotate(Vector3.Y, MathUtils.random(360));
                scenes.add(s);
                sceneManager.addScene(s);
            }
        }
    }

    private void loadFilesWithLoDs() {
        // create scene
        sceneManager = new LoDSceneManager(0);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                // Creating a new sceneAsset in the loop mimics 100 DIFFERENT GLTF files being loaded
                sceneAsset = new GLTFLoader().load(Gdx.files.internal(modelPrefix + ".gltf"));
                LoDScene s = new LoDScene(sceneAsset.scene);
                s.modelInstance.transform.translate(i* 10 - 50, 0, j * 10 - 50);
                s.modelInstance.transform.rotate(Vector3.Y, MathUtils.random(360));
                s.loadLoDs(Gdx.files.internal(modelPrefix + ".lod")); // NEW! Load the LoD meta data
                scenes.add(s);
                sceneManager.addScene(s);
            }
        }
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
        camera.position.set(53f, 32.7f, 53f);
        camera.lookAt(tempVec.set(20,0,20));
        sceneManager.setCamera(camera);

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(12);
        controller.setDegreesPerPixel(0.05f);
        Gdx.input.setInputProcessor(controller);
    }

    private void setupGUI() {
        spriteBatch = new SpriteBatch();
        bitmapFont = new BitmapFont();
        bitmapFont.getData().scale(2);
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
