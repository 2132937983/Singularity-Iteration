package guideme.scene.element;
public interface SceneElementTagCompiler extends guideme.extensions.Extension {
    guideme.extensions.ExtensionPoint<SceneElementTagCompiler> EXTENSION_POINT = new guideme.extensions.ExtensionPoint<>(SceneElementTagCompiler.class);
}
