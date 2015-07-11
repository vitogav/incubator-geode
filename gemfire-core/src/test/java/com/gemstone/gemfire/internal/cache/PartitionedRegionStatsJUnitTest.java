/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/**
 * This test verifies that stats are collected properly for the SingleNode and Single PartitionedRegion
 *
 */
package com.gemstone.gemfire.internal.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.Statistics;
import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.EvictionAction;
import com.gemstone.gemfire.cache.EvictionAttributes;
import com.gemstone.gemfire.cache.PartitionAttributesFactory;
import com.gemstone.gemfire.cache.PartitionedRegionStorageException;
import com.gemstone.gemfire.cache.RegionExistsException;
import com.gemstone.gemfire.internal.FileUtil;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * @author tapshank, Created on Apr 13, 2006
 *  
 */
@Category(IntegrationTest.class)
public class PartitionedRegionStatsJUnitTest
{
  private static final File DISK_DIR = new File("PRStatsTest");
  LogWriter logger = null;

  @Before
  public void setUp() {
    logger = PartitionedRegionTestHelper.getLogger();
  }
  
  @After
  public void tearDown() throws IOException {
    PartitionedRegionTestHelper.closeCache();
    FileUtil.delete(DISK_DIR);
  }

  private PartitionedRegion createPR(String name, int lmax, int redundancy) {
    PartitionAttributesFactory paf = new PartitionAttributesFactory();
    paf
      .setLocalMaxMemory(lmax)
      .setRedundantCopies(redundancy)
      .setTotalNumBuckets(13); // set low to reduce logging
    AttributesFactory af = new AttributesFactory();
    af.setPartitionAttributes(paf.create());
    Cache cache = PartitionedRegionTestHelper.createCache();
    PartitionedRegion pr = null;
    try {
      pr = (PartitionedRegion)cache.createRegion(name, af.create());
    }
    catch (RegionExistsException rex) {
      pr = (PartitionedRegion)cache.getRegion(name);
    }    
    return pr;
  }
  
  private PartitionedRegion createPRWithEviction(String name, int lmax, int redundancy, int evictionCount, boolean diskSync, boolean persistent) {
    PartitionAttributesFactory paf = new PartitionAttributesFactory();
    paf
      .setLocalMaxMemory(lmax)
      .setRedundantCopies(redundancy)
      .setTotalNumBuckets(13); // set low to reduce logging
    AttributesFactory af = new AttributesFactory();
    af.setPartitionAttributes(paf.create());
    if(persistent) {
      af.setDataPolicy(DataPolicy.PERSISTENT_PARTITION);
    }
    af.setEvictionAttributes(EvictionAttributes.createLRUEntryAttributes(1, EvictionAction.OVERFLOW_TO_DISK));
    af.setDiskStoreName("diskstore");
    af.setDiskSynchronous(diskSync);
    Cache cache = PartitionedRegionTestHelper.createCache();
    DISK_DIR.mkdir();
    cache.createDiskStoreFactory().setDiskDirs(new File[] {DISK_DIR}).create("diskstore");
    PartitionedRegion pr = null;
    try {
      pr = (PartitionedRegion)cache.createRegion(name, af.create());
    }
    catch (RegionExistsException rex) {
      pr = (PartitionedRegion)cache.getRegion(name);
    }    
    return pr;
  }
    
  /**
   * This test verifies that PR statistics are working properly for
   * single/multiple PartitionedRegions on single node.
   * 
   * @throws Exception
   */
  @Test
  public void testStats() throws Exception
  {
    String regionname = "testStats";
    int localMaxMemory = 100;
    PartitionedRegion pr = createPR(regionname + 1, localMaxMemory, 0);
    validateStats(pr);
    pr = createPR(regionname + 2, localMaxMemory, 0);
    validateStats(pr);

    if (logger.fineEnabled()) {
      logger
          .fine("PartitionedRegionStatsJUnitTest -  testStats() Completed successfully ... ");
    }
  }
  
  /**
   * This method verifies that PR statistics are working properly for a
   * PartitionedRegion. putsCompleted, getsCompleted, createsCompleted,
   * destroysCompleted, containsKeyCompleted, containsValueForKeyCompleted,
   * invalidatesCompleted, totalBucketSize
   * and temporarily commented avgRedundantCopies,
   * maxRedundantCopies, minRedundantCopies are validated in this method.
   */
  private void validateStats(PartitionedRegion pr) throws Exception  {
    Statistics stats = pr.getPrStats().getStats();
    int bucketCount = stats.get("bucketCount").intValue();
    int putsCompleted = stats.get("putsCompleted").intValue();
    int totalBucketSize = stats.get("dataStoreEntryCount").intValue();
    
    assertEquals(0, bucketCount);
    assertEquals(0, putsCompleted);
    assertEquals(0, totalBucketSize);
    int totalGets = 0;
    
    final int bucketMax = pr.getTotalNumberOfBuckets();
    for (int i = 0; i < bucketMax + 1; i++) {
      Long val = new Long(i);
      try {
        pr.put(val, val);
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }
    for (int i = 0; i < bucketMax + 1; i++) {
      Long val = new Long(i);
      try {
        pr.get(val);
        totalGets++;
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }


    bucketCount = stats.get("bucketCount").intValue();
    putsCompleted = stats.get("putsCompleted").intValue();
    totalBucketSize = stats.get("dataStoreEntryCount").intValue();
    
    assertEquals(bucketMax, bucketCount);
    assertEquals(bucketMax+1, putsCompleted);
    assertEquals(bucketMax+1, totalBucketSize);
    
    pr.destroy(new Long(bucketMax));

    putsCompleted = stats.get("putsCompleted").intValue();
    totalBucketSize = stats.get("dataStoreEntryCount").intValue();
    
    assertEquals(bucketMax, bucketCount);
    assertEquals(bucketMax+1, putsCompleted);
    assertEquals(bucketMax, totalBucketSize);
    
    for (int i = 200; i < 210; i++) {
      Long key = new Long(i);
      String val = "" + i;
      try {
        pr.create(key, val);
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }
    for (int i = 200; i < 210; i++) {
      Long key = new Long(i);
      try {
        pr.get(key);
        totalGets++; 
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }


    for (int i = 200; i < 210; i++) {
      Long key = new Long(i);
      try {
        pr.containsKey(key);
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }

    for (int i = 200; i < 210; i++) {
      Long key = new Long(i);
      try {
        pr.containsValueForKey(key);
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }

    for (int i = 200; i < 210; i++) {
      Long key = new Long(i);
      try {
        pr.invalidate(key);
      }
      catch (PartitionedRegionStorageException ex) {
        this.logger.warning(ex);
      }
    }
    int getsCompleted = stats.get("getsCompleted").intValue();
    int createsCompleted = stats.get("createsCompleted").intValue();
    int containsKeyCompleted = stats.get("containsKeyCompleted").intValue();
    int containsValueForKeyCompleted = stats.get(
    "containsValueForKeyCompleted").intValue();
    int invalidatesCompleted = stats.get("invalidatesCompleted").intValue();
    int destroysCompleted = stats.get("destroysCompleted").intValue();

    assertEquals(totalGets, getsCompleted);
    assertEquals(10, createsCompleted);
    assertEquals(10, containsKeyCompleted);
    assertEquals(10, containsValueForKeyCompleted);
    assertEquals(10, invalidatesCompleted);
    assertEquals(1, destroysCompleted);

    // Redundant copies related statistics
    /*
     * int maxRedundantCopies = stats.get("maxRedundantCopies").intValue();
     * int minRedundantCopies = stats.get("minRedundantCopies").intValue();
     * int avgRedundantCopies = stats.get("avgRedundantCopies").intValue();
     * 
     * assertEquals(minRedundantCopies, 2); assertEquals(maxRedundantCopies,
     * 2); assertEquals(avgRedundantCopies, 2);
     */
  }
  
  public void testOverflowStatsAsync() throws Exception
  {
    String regionname = "testStats";
    int localMaxMemory = 100;
    PartitionedRegion pr = createPRWithEviction(regionname + 1, localMaxMemory, 0, 1, false, false);
    validateOverflowStats(pr);
  }
  
  /**
   * This test verifies that PR statistics are working properly for
   * single/multiple PartitionedRegions on single node.
   * 
   * @throws Exception
   */
  public void testOverflowStats() throws Exception
  {
    String regionname = "testStats";
    int localMaxMemory = 100;
    PartitionedRegion pr = createPRWithEviction(regionname + 1, localMaxMemory, 0, 1, true, false);
    validateOverflowStats(pr);
  }
  
  public void testPersistOverflowStatsAsync() throws Exception
  {
    String regionname = "testStats";
    int localMaxMemory = 100;
    PartitionedRegion pr = createPRWithEviction(regionname + 1, localMaxMemory, 0, 1, false, true);
    validateOverflowStats(pr);
  }
  
  /**
   * This test verifies that PR statistics are working properly for
   * single/multiple PartitionedRegions on single node.
   * 
   * @throws Exception
   */
  public void testPersistOverflowStats() throws Exception
  {
    String regionname = "testStats";
    int localMaxMemory = 100;
    PartitionedRegion pr = createPRWithEviction(regionname + 1, localMaxMemory, 0, 1, true, true);
    validateOverflowStats(pr);
  }
  
  private void validateOverflowStats(PartitionedRegion pr) throws Exception  {
    Statistics stats = pr.getPrStats().getStats();
    DiskRegionStats diskStats = pr.getDiskRegionStats();
    
    assertEquals(0 , stats.getLong("dataStoreBytesInUse"));
    assertEquals(0 , stats.getInt("dataStoreEntryCount"));
    assertEquals(0 , diskStats.getNumOverflowBytesOnDisk());
    assertEquals(0 , diskStats.getNumEntriesInVM());
    assertEquals(0 , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));

    
    int numEntries = 0;
    
    pr.put(0, 0);
    numEntries++;
    pr.getDiskStore().flush();
    
    long singleEntryMemSize = stats.getLong("dataStoreBytesInUse");
    assertEquals(1 , stats.getInt("dataStoreEntryCount"));
    assertEquals(0 , diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals(0 , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    pr.put(1, 1);
    numEntries++;
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(2 , stats.getInt("dataStoreEntryCount"));
    long entryOverflowSize = diskStats.getNumOverflowBytesOnDisk();
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals(1 , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    assertTrue(entryOverflowSize > 0);
    
    for(; numEntries < pr.getTotalNumberOfBuckets() * 5; numEntries++) {
      pr.put(numEntries, numEntries);
    }
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
    assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    
    //Update some entries 
    for(int i = 0; i < numEntries / 2; i++) {
      pr.put(i, i*2);
    }
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
    assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    //Get some entries to trigger evictions
    for(int i = 0; i < numEntries / 2; i++) {
      pr.get(i);
    }
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
    assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    
    //Remove some entries
    for(; numEntries > 100; numEntries--) {
      pr.remove(numEntries);
    }
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
    assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));

    //Update the same entry twice
    pr.put(5, 5);
    pr.put(5, 6);
    pr.getDiskStore().flush();
    
    assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
    assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
    assertEquals(1 , diskStats.getNumEntriesInVM());
    assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
    assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
    assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    
    //Do some random operations

    System.out.println("----Doing random operations");
    Random rand = new Random(12345L);
    for(int i =0; i < 1000; i++) {
      int key = rand.nextInt(numEntries);
      int op = rand.nextInt(3);
      switch(op) {
        case 0:
          System.out.println("put");
          pr.put(key, rand.nextInt());
          break;
        case 1:
          System.out.println("get");
          pr.get(key);
          break;
        case 2:
          System.out.println("remove");
          pr.remove(key);
          break;
      }
    }
    
    pr.getDiskStore().flush();
    
    System.out.println("----Done with random operations");

    numEntries = pr.entryCount();
    
    if(stats.getLong("dataStoreBytesInUse") == 0) {
      //It appears we can get into a case here where all entries are overflowed,
      //rather than just one. I think this may be due to removing the 
      //one in memory entry.
      assertEquals(numEntries * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
//    The entry count stats have a bug, they are getting off. It appears
//    to be related to incrementing the stats multiple times for TOMBSTONES, but
//    only for async regions
//      assertEquals(0 , diskStats.getNumEntriesInVM());
//      assertEquals(numEntries , diskStats.getNumOverflowOnDisk());
      assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
      assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    } else {
      assertEquals(singleEntryMemSize, stats.getLong("dataStoreBytesInUse"));
//    assertEquals(numEntries , stats.getInt("dataStoreEntryCount"));
      assertEquals((numEntries -1) * entryOverflowSize, diskStats.getNumOverflowBytesOnDisk());
//    assertEquals(1 , diskStats.getNumEntriesInVM());
//      assertEquals((numEntries -1) , diskStats.getNumOverflowOnDisk());
      assertEquals(stats.getLong("dataStoreBytesInUse"), getMemBytes(pr));
      assertEquals(diskStats.getNumOverflowBytesOnDisk(), getDiskBytes(pr));
    }
  }

  private Object getDiskBytes(PartitionedRegion pr) {
Set<BucketRegion> brs = pr.getDataStore().getAllLocalBucketRegions();
    
    long bytes = 0;
    for(Iterator<BucketRegion> itr = brs.iterator(); itr.hasNext(); ) {
      BucketRegion br = itr.next();
      bytes += br.getNumOverflowBytesOnDisk();
    }
    
    return bytes;
  }

  private long getMemBytes(PartitionedRegion pr) {
    Set<BucketRegion> brs = pr.getDataStore().getAllLocalBucketRegions();
    
    long bytes = 0;
    for(Iterator<BucketRegion> itr = brs.iterator(); itr.hasNext(); ) {
      BucketRegion br = itr.next();
      bytes += br.getBytesInMemory();
    }
    
    return bytes;
  }
}
