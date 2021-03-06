/*=========================================================================
 * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal;

import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.test.junit.categories.UnitTest;


/**
 * See bug 52093.
 * Make sure that the contents of arrays are
 * used to test equality on regions ops.
 */
@Category(UnitTest.class)
public class ArrayEqualsJUnitTest {
  private GemFireCacheImpl createCache() {
    Properties props = new Properties();
    props.setProperty("locators", "");
    props.setProperty("mcast-port", "0");
    GemFireCacheImpl result = (GemFireCacheImpl) new CacheFactory(props).create();
    return result;
  }
  private void closeCache(GemFireCacheImpl gfc) {
    gfc.close();
  }
  
  private void doOps(Region r) {
    {
      byte[] bytesValue = new byte[]{1,2,3,4};
      r.put("bytesValue", bytesValue.clone());
      if (r.replace("bytesValue", "", "")) fail("expected replace to fail");
      if (!r.replace("bytesValue", bytesValue, "")) {
        fail("expected replace to happen");
      }
      r.put("bytesValue", bytesValue.clone());
      if (r.remove("bytesValue", "")) fail("expected remove to fail");
      if (!r.remove("bytesValue", bytesValue)) {
        fail("expected remove to happen");
      }
    }
    {
      boolean[] booleanValue = new boolean[]{true,false,true,false};
      r.put("booleanValue", booleanValue.clone());
      if (r.replace("booleanValue", "", "")) fail("expected replace to fail");
      if (!r.replace("booleanValue", booleanValue, "")) {
        fail("expected replace to happen");
      }
      r.put("booleanValue", booleanValue.clone());
      if (r.remove("booleanValue", "")) fail("expected remove to fail");
      if (!r.remove("booleanValue", booleanValue)) {
        fail("expected remove to happen");
      }
    }
    {
      short[] shortValue = new short[]{1,2,3,4};
      r.put("shortValue", shortValue.clone());
      if (r.replace("shortValue", "", "")) fail("expected replace to fail");
      if (!r.replace("shortValue", shortValue, "")) {
        fail("expected replace to happen");
      }
      r.put("shortValue", shortValue.clone());
      if (r.remove("shortValue", "")) fail("expected remove to fail");
      if (!r.remove("shortValue", shortValue)) {
        fail("expected remove to happen");
      }
    }
    {
      char[] charValue = new char[]{1,2,3,4};
      r.put("charValue", charValue.clone());
      if (r.replace("charValue", "", "")) fail("expected replace to fail");
      if (!r.replace("charValue", charValue, "")) {
        fail("expected replace to happen");
      }
      r.put("charValue", charValue.clone());
      if (r.remove("charValue", "")) fail("expected remove to fail");
      if (!r.remove("charValue", charValue)) {
        fail("expected remove to happen");
      }
    }
    {
      int[] intValue = new int[]{1,2,3,4};
      r.put("intValue", intValue.clone());
      if (r.replace("intValue", "", "")) fail("expected replace to fail");
      if (!r.replace("intValue", intValue, "")) {
        fail("expected replace to happen");
      }
      r.put("intValue", intValue.clone());
      if (r.remove("intValue", "")) fail("expected remove to fail");
      if (!r.remove("intValue", intValue)) {
        fail("expected remove to happen");
      }
    }
    {
      long[] longValue = new long[]{1,2,3,4};
      r.put("longValue", longValue.clone());
      if (r.replace("longValue", "", "")) fail("expected replace to fail");
      if (!r.replace("longValue", longValue, "")) {
        fail("expected replace to happen");
      }
      r.put("longValue", longValue.clone());
      if (r.remove("longValue", "")) fail("expected remove to fail");
      if (!r.remove("longValue", longValue)) {
        fail("expected remove to happen");
      }
    }
    {
      float[] floatValue = new float[]{1,2,3,4};
      r.put("floatValue", floatValue.clone());
      if (r.replace("floatValue", "", "")) fail("expected replace to fail");
      if (!r.replace("floatValue", floatValue, "")) {
        fail("expected replace to happen");
      }
      r.put("floatValue", floatValue.clone());
      if (r.remove("floatValue", "")) fail("expected remove to fail");
      if (!r.remove("floatValue", floatValue)) {
        fail("expected remove to happen");
      }
    }
    {
      double[] doubleValue = new double[]{1,2,3,4};
      r.put("doubleValue", doubleValue.clone());
      if (r.replace("doubleValue", "", "")) fail("expected replace to fail");
      if (!r.replace("doubleValue", doubleValue, "")) {
        fail("expected replace to happen");
      }
      r.put("doubleValue", doubleValue.clone());
      if (r.remove("doubleValue", "")) fail("expected remove to fail");
      if (!r.remove("doubleValue", doubleValue)) {
        fail("expected remove to happen");
      }
    }
    {
      Object[] oaValue = new Object[]{new byte[]{1,2,3,4},new short[]{1,2,3,4},new int[]{1,2,3,4}, "hello sweet world!"};
      r.put("oaValue", oaValue);
      Object[] deepCloneOaValue = new Object[]{new byte[]{1,2,3,4},new short[]{1,2,3,4},new int[]{1,2,3,4}, "hello sweet world!"};
      if (r.replace("oaValue", "", "")) fail("expected replace to fail");
      if (!r.replace("oaValue", deepCloneOaValue, "")) {
        fail("expected replace to happen");
      }
      r.put("oaValue", oaValue);
      if (r.remove("oaValue", "")) fail("expected remove to fail");
      if (!r.remove("oaValue", deepCloneOaValue)) {
        fail("expected remove to happen");
      }
    }
  }
  
  @Test
  public void testPartition() {
    GemFireCacheImpl gfc = createCache();
    try {
      Region r = gfc.createRegionFactory(RegionShortcut.PARTITION).create("ArrayEqualsJUnitTestPartitionRegion");
      doOps(r);
    } finally {
      closeCache(gfc);
    }
  }
  @Test
  public void testLocal() {
    GemFireCacheImpl gfc = createCache();
    try {
      Region r = gfc.createRegionFactory(RegionShortcut.LOCAL).create("ArrayEqualsJUnitTestLocalRegion");
      doOps(r);
    } finally {
      closeCache(gfc);
    }
  }

}
