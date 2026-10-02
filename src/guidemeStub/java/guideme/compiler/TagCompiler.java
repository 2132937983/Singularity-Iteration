package guideme.compiler;
public interface TagCompiler extends guideme.extensions.Extension {
    guideme.extensions.ExtensionPoint<TagCompiler> EXTENSION_POINT = new guideme.extensions.ExtensionPoint<>(TagCompiler.class);
}
