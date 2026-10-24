#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;

uniform vec2 CullingSize;
uniform mat4 CullingViewMat;
uniform mat4 CullingProjMat;
uniform vec3 CullingCameraPos;
uniform vec3 CullingCameraDir;
uniform vec3 FrustumPos;
uniform float RenderDistance;
uniform int LevelHeightOffset;
uniform int LevelMinSection;
uniform float BoxScale;

flat in int spacePartitionSize;
flat in vec4 frustum[6];
flat in vec2 DepthScreenSize[5];

out vec4 fragColor;

float near = 0.1;
float far  = 1000.0;

int getSampler(float xLength, float yLength) {
    int count = DepthScreenSize.length();
    for(int i = 0; i < count; ++i) {
        float xStep = 2.0 / DepthScreenSize[i].x;
        float yStep = 2.0 / DepthScreenSize[i].y;
        if(xStep > xLength && yStep > yLength) {
            return i;
        }
    }
    return count - 1;
}

float LinearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (near * far) / (far + near - z * (far - near));
}

float calculateDistance(vec3 P, vec3 Q) {
    vec3 d = Q - P;
    return dot(d, d);
}

vec3 worldToScreenSpace(vec3 pos) {
    vec4 cameraSpace = CullingProjMat * CullingViewMat * vec4(pos, 1.0);
    vec3 ndc = cameraSpace.xyz / cameraSpace.w;
    return (ndc + vec3(1.0)) * 0.5;
}

vec3 moveTowardsCamera(vec3 pos, float distanceVal) {
    vec3 direction = normalize(pos - CullingCameraPos);
    vec3 newPos = pos - direction * distanceVal;
    return newPos;
}

vec3 blockToChunk(vec3 blockPos) {
    vec3 chunkPos;
    chunkPos.x = floor(blockPos.x / 16.0);
    chunkPos.y = floor(blockPos.y / 16.0);
    chunkPos.z = floor(blockPos.z / 16.0);
    return chunkPos;
}

bool cubeInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    for(int i = 0; i < 6; ++i) {
        if (!(dot(frustum[i], vec4(minX, minY, minZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(maxX, minY, minZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(minX, maxY, minZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(maxX, maxY, minZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(minX, minY, maxZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(maxX, minY, maxZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(minX, maxY, maxZ, 1.0)) > 0.0) &&
        !(dot(frustum[i], vec4(maxX, maxY, maxZ, 1.0)) > 0.0)) {
            return false;
        }
    }
    return true;
}

bool calculateCube(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    float f = minX - FrustumPos.x;
    float f1 = minY - FrustumPos.y;
    float f2 = minZ - FrustumPos.z;
    float f3 = maxX - FrustumPos.x;
    float f4 = maxY - FrustumPos.y;
    float f5 = maxZ - FrustumPos.z;
    return cubeInFrustum(f, f1, f2, f3, f4, f5);
}

float unpackDepth(vec2 e) {
    return e.x + e.y / 255.0;
}

float getUVDepth(int idx, vec2 uv) {
    if(idx == 0) return unpackDepth(texture(Sampler0, uv).rg) * 500.0;
    else if(idx == 1) return unpackDepth(texture(Sampler1, uv).rg) * 500.0;
    else if(idx == 2) return unpackDepth(texture(Sampler2, uv).rg) * 500.0;
    else if(idx == 3) return unpackDepth(texture(Sampler3, uv).rg) * 500.0;
    return unpackDepth(texture(Sampler4, uv).rg) * 500.0;
}

const float MAX_SAFE_MOVEMENT = 32.0;
const int GAP_SEARCH_RINGS = 4;

float unpackMinDepth(vec4 texel) {
    return unpackDepth(texel.ba) * 500.0;
}

float getUVMinDepth(int idx, vec2 uv) {
    if(idx == 0) return unpackMinDepth(texture(Sampler0, uv));
    else if(idx == 1) return unpackMinDepth(texture(Sampler1, uv));
    else if(idx == 2) return unpackMinDepth(texture(Sampler2, uv));
    else if(idx == 3) return unpackMinDepth(texture(Sampler3, uv));
    return unpackMinDepth(texture(Sampler4, uv));
}

// True when the depth texel at uv could reveal the chunk: something in it is further away than the chunk, or it is off
// screen, where nothing is known. Otherwise the texel is solid occluder and its nearest depth lowers nearestOccluder.
bool isGap(int idx, vec2 uv, float chunkDepth, inout float nearestOccluder) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return true;
    if (chunkDepth < getUVDepth(idx, uv)) return true;
    nearestOccluder = min(nearestOccluder, getUVMinDepth(idx, uv));
    return false;
}

// Camera movement, in blocks, that a culled chunk is guaranteed to stay hidden for. Moving the camera by D shifts an
// occluder at depth z across the screen by at most D / z (in tangent space), so when the r texel rings around the
// chunk's footprint are all solid, the camera has to move at least r * texel * z before a gap can reach the chunk.
// Fine levels resolve gaps close to the footprint, coarse levels prove large solid areas cheaply, so several levels are
// tried and the best bound kept. Halved to cover forward motion and depth linearisation error.
float safeMovement(int idx, float minX, float maxX, float minY, float maxY, float chunkDepth, float nearestOccluder) {
    float best = 0.0;
    for (int level = max(idx - 2, 0); level < 5; ++level) {
        float sx = 1.0 / DepthScreenSize[level].x;
        float sy = 1.0 / DepthScreenSize[level].y;
        // Texel centres covering the footprint on this level.
        float x0 = (floor(minX / sx) + 0.5) * sx;
        float x1 = (floor(min(maxX, 0.9999) / sx) + 0.5) * sx;
        float y0 = (floor(minY / sy) + 0.5) * sy;
        float y1 = (floor(min(maxY, 0.9999) / sy) + 0.5) * sy;

        // Finer levels than the one that culled the chunk are solid over the footprint by construction; coarser ones
        // mix in surroundings and have to be checked. Once a level isn't solid, no coarser one is either.
        if (level > idx) {
            bool solid = true;
            for (float x = x0; x <= x1 + sx * 0.5 && solid; x += sx) {
                for (float y = y0; y <= y1 + sy * 0.5 && solid; y += sy) {
                    solid = !isGap(level, vec2(x, y), chunkDepth, nearestOccluder);
                }
            }
            if (!solid) break;
        }

        int rings = 0;
        for (int k = 1; k <= GAP_SEARCH_RINGS; ++k) {
            float rx0 = x0 - float(k) * sx;
            float rx1 = x1 + float(k) * sx;
            float ry0 = y0 - float(k) * sy;
            float ry1 = y1 + float(k) * sy;
            bool gap = false;
            for (float x = rx0; x <= rx1 + sx * 0.5 && !gap; x += sx) {
                gap = isGap(level, vec2(x, ry0), chunkDepth, nearestOccluder) || isGap(level, vec2(x, ry1), chunkDepth, nearestOccluder);
            }
            for (float y = ry0 + sy; y < ry1 - sy * 0.5 && !gap; y += sy) {
                gap = isGap(level, vec2(rx0, y), chunkDepth, nearestOccluder) || isGap(level, vec2(rx1, y), chunkDepth, nearestOccluder);
            }
            if (gap) break;
            rings = k;
        }

        float texelTan = min(2.0 * sx / CullingProjMat[0][0], 2.0 * sy / CullingProjMat[1][1]);
        best = max(best, float(rings) * texelTan * nearestOccluder);
    }
    return clamp(0.5 * best, 0.0, MAX_SAFE_MOVEMENT);
}

void main() {
    int screenIndex = int(gl_FragCoord.x) + int(gl_FragCoord.y) * int(CullingSize.x);
    int rDist = int(RenderDistance);
    int localSpacePartitionSize = rDist * 2 + 1;

    int chunkX = screenIndex / (localSpacePartitionSize * LevelHeightOffset) - rDist;
    int chunkZ = (screenIndex / LevelHeightOffset) % localSpacePartitionSize - rDist;
    int chunkY = screenIndex % LevelHeightOffset + LevelMinSection;

    vec3 chunkBasePos = vec3(float(chunkX), float(chunkY), float(chunkZ));
    vec3 chunkPos = (chunkBasePos + blockToChunk(CullingCameraPos)) * 16.0;
    chunkPos = vec3(chunkPos.x, float(chunkY) * 16.0, chunkPos.z) + vec3(8.0);

    float chunkCenterDepth = worldToScreenSpace(moveTowardsCamera(chunkPos, 16.0)).z;
    if (calculateDistance(chunkPos, CullingCameraPos) < 1024.0) {
        fragColor = vec4(1.0, 1.0, 1.0, 1.0);
        return;
    }

    float chunkDepth = LinearizeDepth(chunkCenterDepth) - BoxScale;

    if(chunkDepth < 0.0) {
        fragColor = vec4(0.0, 0.4, 0.5, 1.0);
        return;
    }

    float sizeOffset = 8.0;
    vec3 aabb[8] = vec3[](
    chunkPos + vec3(-sizeOffset, -sizeOffset, -sizeOffset), chunkPos + vec3(sizeOffset, -sizeOffset, -sizeOffset),
    chunkPos + vec3(-sizeOffset, sizeOffset, -sizeOffset), chunkPos + vec3(sizeOffset, sizeOffset, -sizeOffset),
    chunkPos + vec3(-sizeOffset, -sizeOffset, sizeOffset), chunkPos + vec3(sizeOffset, -sizeOffset, sizeOffset),
    chunkPos + vec3(-sizeOffset, sizeOffset, sizeOffset), chunkPos + vec3(sizeOffset, sizeOffset, sizeOffset)
    );

    float maxX = -0.1;
    float maxY = -0.1;
    float minX = 1.1;
    float minY = 1.1;

    bool inside = false;
    vec3 column0 = CullingViewMat[0].xyz;
    vec3 column1 = CullingViewMat[1].xyz;
    vec3 column2 = CullingViewMat[2].xyz;

    vec3 cameraUp = vec3(column0.y, column1.y, column2.y);
    vec3 cameraRight = vec3(column0.x, column1.x, column2.x);

    for (int i = 0; i < 8; ++i) {
        vec3 screenPos = worldToScreenSpace(aabb[i]);
        if (screenPos.x >= 0.0 && screenPos.x <= 1.0
        && screenPos.y >= 0.0 && screenPos.y <= 1.0
        && screenPos.z >= 0.0 && screenPos.z <= 1.0) {
            inside = true;
        } else {
            vec3 vectorDir = normalize(aabb[i] - CullingCameraPos);

            float xDot = dot(vectorDir, cameraRight);
            if (xDot < 0.0 && screenPos.x > 0.5) screenPos.x = 0.0;
            if (xDot > 0.0 && screenPos.x < 0.5) screenPos.x = 1.0;

            float yDot = dot(vectorDir, cameraUp);
            if (yDot < 0.0 && screenPos.y > 0.5) screenPos.y = 0.0;
            if (yDot > 0.0 && screenPos.y < 0.5) screenPos.y = 1.0;
        }

        if (screenPos.x > maxX) maxX = screenPos.x;
        if (screenPos.y > maxY) maxY = screenPos.y;
        if (screenPos.x < minX) minX = screenPos.x;
        if (screenPos.y < minY) minY = screenPos.y;
    }

    if(!inside) {
        fragColor = vec4(1.0, 1.0, 0.0, 1.0);
        return;
    }

    int idx = getSampler(maxX - minX, maxY - minY);

    float xStep = 1.0 / DepthScreenSize[idx].x;
    float yStep = 1.0 / DepthScreenSize[idx].y;

    minX = max(minX - xStep, 0.0);
    maxX = min(maxX + xStep, 1.0);
    minY = max(minY - yStep, 0.0);
    maxY = min(maxY + yStep, 1.0);

    float nearestOccluder = chunkDepth;
    for(float x = minX; x <= maxX; x += xStep) {
        for(float y = minY; y <= maxY; y += yStep) {
            float pixelDepth = getUVDepth(idx, vec2(x, y));
            if(chunkDepth < pixelDepth) {
                fragColor = vec4(0.0, 1.0, 0.0, 1.0);
                return;
            }
            nearestOccluder = min(nearestOccluder, getUVMinDepth(idx, vec2(x, y)));
        }
    }

    // Culled. The verdict only holds for the camera position this map was drawn from, and it is read back a few
    // frames later. Find the nearest gap around the chunk's footprint and store how far the camera can move before
    // parallax could slide that gap over the chunk.
    // Stored as a square root so short distances, the common case near walls, keep their precision in 8 bits.
    fragColor = vec4(sqrt(safeMovement(idx, minX, maxX, minY, maxY, chunkDepth, nearestOccluder) / MAX_SAFE_MOVEMENT), 0.0, 0.0, 1.0);
}
