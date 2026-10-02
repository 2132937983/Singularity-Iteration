package guideme;
/** Test-only stand-in for GuideME: lets MI/AE2 load on a headless GameTest server. */
public interface Guide {
    static GuideBuilder builder(net.minecraft.resources.ResourceLocation id) { return new GuideBuilder(id); }
    net.minecraft.resources.ResourceLocation getId();
}
