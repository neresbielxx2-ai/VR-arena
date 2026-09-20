package com.lunarvr

import com.lunarvr.handtracking.BoundingBox3D
import com.lunarvr.handtracking.Ray3D
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionManagerTest {

    @Test
    fun testRayIntersection() {
        val box = BoundingBox3D(
            minX = -0.5f,
            maxX = 0.5f,
            minY = -0.5f,
            maxY = 0.5f,
            minZ = -1.5f,
            maxZ = -0.5f
        )

        // Forward ray hitting center (targetZ = -1.0)
        val hitRay = Ray3D(
            originX = 0f,
            originY = 0f,
            originZ = 0f,
            dirX = 0f,
            dirY = 0f,
            dirZ = -1f
        )
        assertTrue(box.intersects(hitRay))

        // Ray pointing sideways missing the box
        val missRay = Ray3D(
            originX = 0f,
            originY = 0f,
            originZ = 0f,
            dirX = 1f,
            dirY = 0f,
            dirZ = 0f
        )
        assertFalse(box.intersects(missRay))
    }
    @Test
    fun testAllClassesCompile() {
        // Simple test to compile references in test phase
        val pos = com.lunarvr.environment.CameraPosition(1f, 2f, 3f)
        org.junit.Assert.assertEquals(1f, pos.posX)
    }
}
