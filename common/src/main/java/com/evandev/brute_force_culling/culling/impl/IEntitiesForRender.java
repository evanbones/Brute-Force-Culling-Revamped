package com.evandev.brute_force_culling.culling.impl;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

import java.util.List;

public interface IEntitiesForRender {
    List<SectionRenderDispatcher.RenderSection> bruteForceCulling$visibleSections();
}
