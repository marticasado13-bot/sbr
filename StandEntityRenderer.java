package com.steelballrun;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Renderiza el Stand como una figura humanoide translúcida (un color por tipo). */
public class StandEntityRenderer extends MobEntityRenderer<StandEntity, BipedEntityModel<StandEntity>> {
    private static final Identifier[] TEXTURES = new Identifier[StandType.values().length];
    static {
        for (StandType t : StandType.values())
            TEXTURES[t.ordinal()] = new Identifier(SteelBallRun.MOD_ID, "textures/entity/stand_" + t.id + ".png");
    }

    public StandEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.0f);
    }

    @Override
    public Identifier getTexture(StandEntity entity) { return TEXTURES[entity.getVariant()]; }

    @Override
    protected RenderLayer getRenderLayer(StandEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }
}
