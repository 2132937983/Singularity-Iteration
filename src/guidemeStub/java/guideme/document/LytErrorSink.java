package guideme.document;
public interface LytErrorSink {
    void appendError(guideme.compiler.PageCompiler compiler, String text, guideme.libs.unist.UnistNode node);
}
