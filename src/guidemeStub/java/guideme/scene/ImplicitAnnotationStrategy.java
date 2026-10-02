package guideme.scene;
public interface ImplicitAnnotationStrategy extends guideme.extensions.Extension {
    guideme.extensions.ExtensionPoint<ImplicitAnnotationStrategy> EXTENSION_POINT = new guideme.extensions.ExtensionPoint<>(ImplicitAnnotationStrategy.class);
}
