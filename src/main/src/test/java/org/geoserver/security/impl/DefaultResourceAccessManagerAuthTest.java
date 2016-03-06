/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;


public class DefaultResourceAccessManagerAuthTest extends AbstractAuthorizationTest {

    @Test 
    public void testWideOpen() throws Exception {
        ResourceAccessManager manager = buildAccessManager("wideOpen.properties");
        checkUserAccessFlat(manager, anonymous, true, true);
    }

    @Test
    public void testLockedDown() throws Exception {
        ResourceAccessManager manager = buildAccessManager("lockedDown.properties");
        checkUserAccessFlat(manager, anonymous, false, false);
        checkUserAccessFlat(manager, roUser, false, false);
        checkUserAccessFlat(manager, rwUser, true, true);
        checkUserAccessFlat(manager, root, true, true);
    }
    
    @Test
    public void testPublicRead() throws Exception {
        ResourceAccessManager manager = buildAccessManager("publicRead.properties");
        checkUserAccessFlat(manager, anonymous, true, false);
        checkUserAccessFlat(manager, roUser, true, false);
        checkUserAccessFlat(manager, rwUser, true, true);
        checkUserAccessFlat(manager, root, true, true);
    }
    
    private void checkUserAccessFlat(ResourceAccessManager manager, Authentication user, boolean expectedRead, boolean expectedWrite) {
        // states as a layer
        assertEquals(expectedRead, canAccess(manager, user, statesLayer, AccessMode.READ));
        assertEquals(expectedWrite, canAccess(manager, user, statesLayer, AccessMode.WRITE));
        // states as a resource
        final ResourceInfo resource = statesLayer.getResource();
        assertEquals(expectedRead, canAccess(manager, user, resource, AccessMode.READ));
        assertEquals(expectedWrite, canAccess(manager, user, resource, AccessMode.WRITE));
        // the topp ws
        assertEquals(expectedRead, canAccess(manager, user, toppWs, AccessMode.READ));
        assertEquals(expectedWrite, canAccess(manager, user, toppWs, AccessMode.WRITE));
    }
    
    @Test
    public void testComplex() throws Exception {
        ResourceAccessManager wo = buildAccessManager("complex.properties");
        
        // check non configured ws inherits root configuration, auth read, nobody write
        assertFalse(canAccess(wo, anonymous, nurcWs, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, nurcWs, AccessMode.WRITE));
        assertTrue(canAccess(wo, roUser, nurcWs, AccessMode.READ));
        assertFalse(canAccess(wo, rwUser, nurcWs, AccessMode.WRITE));
        assertTrue(canAccess(wo, root, nurcWs, AccessMode.WRITE));
        
        // check access to the topp workspace (everybody read, nobody for write)
        assertTrue(canAccess(wo, anonymous, toppWs, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, toppWs, AccessMode.WRITE));
        assertTrue(canAccess(wo, roUser, toppWs, AccessMode.READ));
        assertFalse(canAccess(wo, rwUser, toppWs, AccessMode.WRITE));
        
        // check non configured layer in topp ws inherits topp security attributes
        assertTrue(canAccess(wo, anonymous, roadsLayer, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, roadsLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, roUser, roadsLayer, AccessMode.READ));
        assertFalse(canAccess(wo, rwUser, roadsLayer, AccessMode.WRITE));
        
        // check states uses its own config (auth for read, auth for write)
        assertFalse(canAccess(wo, anonymous, statesLayer, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, statesLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, roUser, statesLayer, AccessMode.READ));
        assertFalse(canAccess(wo, roUser, statesLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, rwUser, statesLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, rwUser, statesLayer, AccessMode.WRITE));
        
        // check landmarks uses its own config (all can for read, auth for write)
        assertTrue(canAccess(wo, anonymous, landmarksLayer, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, landmarksLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, roUser, landmarksLayer, AccessMode.READ));
        assertFalse(canAccess(wo, roUser, landmarksLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, rwUser, landmarksLayer, AccessMode.READ));
        assertTrue(canAccess(wo, rwUser, statesLayer, AccessMode.WRITE));
        
        // check military is off limits for anyone but the military users
        assertFalse(canAccess(wo, anonymous, basesLayer, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, basesLayer, AccessMode.WRITE));
        assertFalse(canAccess(wo, roUser, basesLayer, AccessMode.READ));
        assertFalse(canAccess(wo, roUser, basesLayer, AccessMode.WRITE));
        assertFalse(canAccess(wo, rwUser, basesLayer, AccessMode.READ));
        assertFalse(canAccess(wo, rwUser, basesLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, milUser, basesLayer, AccessMode.READ));
        assertTrue(canAccess(wo, milUser, basesLayer, AccessMode.WRITE));
        
        // check the layer with dots
        assertFalse(canAccess(wo, anonymous, arcGridLayer, AccessMode.READ));
        assertFalse(canAccess(wo, anonymous, arcGridLayer, AccessMode.WRITE));
        assertFalse(canAccess(wo, roUser, arcGridLayer, AccessMode.READ));
        assertFalse(canAccess(wo, roUser, arcGridLayer, AccessMode.WRITE));
        assertFalse(canAccess(wo, rwUser, arcGridLayer, AccessMode.READ));
        assertFalse(canAccess(wo, rwUser, arcGridLayer, AccessMode.WRITE));
        assertTrue(canAccess(wo, milUser, arcGridLayer, AccessMode.READ));
        assertTrue(canAccess(wo, milUser, arcGridLayer, AccessMode.WRITE));
    }
    
    @Test
    public void testDefaultMode() throws Exception {
        DefaultResourceAccessManager wo = buildAccessManager("lockedDown.properties");
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
    @Test
    public void testHideMode() throws Exception {
        DefaultResourceAccessManager wo = buildAccessManager("lockedDownHide.properties");
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
    @Test
    public void testChallengeMode() throws Exception {
        DefaultResourceAccessManager wo = buildAccessManager("lockedDownChallenge.properties");
        assertEquals(CatalogMode.CHALLENGE, wo.getMode());
    }
    
    @Test
    public void testMixedMode() throws Exception {
        DefaultResourceAccessManager wo = buildAccessManager("lockedDownMixed.properties");
        assertEquals(CatalogMode.MIXED, wo.getMode());
    }
    
    @Test
    public void testUnknownMode() throws Exception {
        DefaultResourceAccessManager wo = buildAccessManager("lockedDownUnknown.properties");
        // should fall back on the default and complain in the logger
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
    @Test
    public void testOverride() throws Exception {
        DefaultResourceAccessManager manager = buildAccessManager("override-ws.properties");
        // since the use can read states, it can read its container ws oo
        assertTrue(canAccess(manager, roUser, statesLayer, AccessMode.READ));
        assertTrue(canAccess(manager, roUser, toppWs, AccessMode.READ));
        // no access for this one
        assertFalse(canAccess(manager, milUser, statesLayer, AccessMode.READ));
        assertFalse(canAccess(manager, milUser, toppWs, AccessMode.READ));
    }
    
    private boolean canAccess(ResourceAccessManager manager, Authentication user, LayerInfo catalogInfo, AccessMode mode) {
        DataAccessLimits limits = manager.getAccessLimits(user, catalogInfo);
        return canAccess(mode, limits);
    }

    private boolean canAccess(AccessMode mode, DataAccessLimits limits) {
        if(limits == null) {
            return true;
        } else if(mode == AccessMode.READ) {
            return limits.getReadFilter() != Filter.EXCLUDE;
        } else if(mode == AccessMode.WRITE) {
            if(limits instanceof VectorAccessLimits) {
                return ((VectorAccessLimits) limits).getWriteFilter() != Filter.EXCLUDE;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Unknown access mode " + mode);
        }
    }
    
    private boolean canAccess(ResourceAccessManager manager, Authentication user, ResourceInfo catalogInfo, AccessMode mode) {
        DataAccessLimits limits = manager.getAccessLimits(user, catalogInfo);
        return canAccess(mode, limits);
    }
    
    private boolean canAccess(ResourceAccessManager manager, Authentication user, WorkspaceInfo catalogInfo, AccessMode mode) {
        WorkspaceAccessLimits limits = manager.getAccessLimits(user, catalogInfo);
        if(limits == null) {
            return true;
        } else if(mode == AccessMode.READ) {
            return limits.isReadable();
        } else if(mode == AccessMode.WRITE) {
            return limits.isWritable();
        } else if(mode == AccessMode.ADMIN) {
            return limits.isAdminable();
        } else {
            throw new RuntimeException("Unknown access mode " + mode);
        }
    }
    
}
