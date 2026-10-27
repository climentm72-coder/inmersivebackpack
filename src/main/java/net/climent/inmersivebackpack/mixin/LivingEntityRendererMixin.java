@SuppressWarnings("unchecked")
@Inject(method = "<init>", at = @At("RETURN"))
private void addBackpackFeature(EntityRendererFactory.Context ctx, M model, float shadowRadius, CallbackInfo ci) {
    LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
    renderer.addFeature(new BackpackFeatureRenderer(renderer, ctx.getItemRenderer()));
}
