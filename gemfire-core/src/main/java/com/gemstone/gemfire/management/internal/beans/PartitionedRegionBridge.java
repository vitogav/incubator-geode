/*
 *  =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *  ========================================================================
 */
package com.gemstone.gemfire.management.internal.beans;

import java.util.Set;

import com.gemstone.gemfire.cache.PartitionAttributes;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.internal.cache.BucketRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegionStats;
import com.gemstone.gemfire.management.FixedPartitionAttributesData;
import com.gemstone.gemfire.management.PartitionAttributesData;
import com.gemstone.gemfire.management.internal.ManagementConstants;
import com.gemstone.gemfire.management.internal.beans.stats.MBeanStatsMonitor;
import com.gemstone.gemfire.management.internal.beans.stats.StatType;
import com.gemstone.gemfire.management.internal.beans.stats.StatsAverageLatency;
import com.gemstone.gemfire.management.internal.beans.stats.StatsKey;
import com.gemstone.gemfire.management.internal.beans.stats.StatsLatency;
import com.gemstone.gemfire.management.internal.beans.stats.StatsRate;

/**
 * 
 * @author rishim
 */
public class PartitionedRegionBridge<K, V>  extends RegionMBeanBridge<K, V> {  

  private PartitionedRegionStats prStats;
  
  private PartitionedRegion parRegion;
  
  private PartitionAttributesData partitionAttributesData;    
  
  private FixedPartitionAttributesData[] fixedPartitionAttributesTable;
  
  private int configuredRedundancy = -1;
  
  private MBeanStatsMonitor parRegionMonitor;
  
  private StatsRate putAllRate;
  
  private StatsRate putRequestRate;
  
  private StatsRate getRequestRate;
  
  private StatsRate createsRate;
  
  private StatsRate destroysRate;
  
  private StatsRate putLocalRate;

  private StatsRate putRemoteRate;

  private StatsLatency putRemoteLatency;
  
  private StatsRate averageWritesRate;
  
  private StatsRate averageReadsRate;

  private StatsAverageLatency remotePutAvgLatency;

  public static final String PAR_REGION_MONITOR = "PartitionedRegionMonitor";
  
  
  public static <K, V> PartitionedRegionBridge<K, V> getInstance(Region<K, V> region) {

    if (region.getAttributes().getDataPolicy().withHDFS()) {
      PartitionedRegionBridge<K, V> bridge = new HDFSRegionBridge<K, V>(region);
      return bridge;
    } else {
      return new PartitionedRegionBridge<K, V> (region);
    }

  }
  
  
  
  protected PartitionedRegionBridge(Region<K, V> region) {    
    super(region);
    this.parRegion = (PartitionedRegion)region;
    this.prStats = parRegion.getPrStats();
    
    PartitionAttributes<K, V>  partAttrs = parRegion.getPartitionAttributes();    
    
    this.parRegionMonitor = new MBeanStatsMonitor(PAR_REGION_MONITOR);
    
    this.configurePartitionRegionMetrics();

    this.configuredRedundancy = partAttrs.getRedundantCopies();
    this.partitionAttributesData = RegionMBeanCompositeDataFactory.getPartitionAttributesData(partAttrs);
    if (partAttrs.getFixedPartitionAttributes() != null) {
      this.fixedPartitionAttributesTable = RegionMBeanCompositeDataFactory.getFixedPartitionAttributesData(partAttrs);
    }
    parRegionMonitor.addStatisticsToMonitor(prStats.getStats());
  }
  
  // Dummy constructor for testing purpose only
  public PartitionedRegionBridge(PartitionedRegionStats prStats) {
    this.prStats = prStats;
    
    this.parRegionMonitor = new MBeanStatsMonitor(PAR_REGION_MONITOR);
    parRegionMonitor.addStatisticsToMonitor(prStats.getStats());
    configurePartitionRegionMetrics();
  }
  
  private Number getPrStatistic(String statName) {
    if (prStats != null) {
      return prStats.getStats().get(statName);
    } else {
      return ManagementConstants.ZERO;
    }
  }

  public void stopMonitor(){
    super.stopMonitor();
    parRegionMonitor.stopListener();
  }

  private void configurePartitionRegionMetrics() {
    putAllRate = new StatsRate(StatsKey.PUTALL_COMPLETED, StatType.INT_TYPE, parRegionMonitor);
    putRequestRate = new StatsRate(StatsKey.PUTS_COMPLETED, StatType.INT_TYPE, parRegionMonitor);
    getRequestRate = new StatsRate(StatsKey.GETS_COMPLETED, StatType.INT_TYPE, parRegionMonitor);
    destroysRate = new StatsRate(StatsKey.DESTROYS_COMPLETED, StatType.INT_TYPE, parRegionMonitor);    
    createsRate = new StatsRate(StatsKey.CREATES_COMPLETED, StatType.INT_TYPE, parRegionMonitor);

    // Remote Reads Only in case of partitioned Region
    putRemoteRate = new StatsRate(StatsKey.REMOTE_PUTS, StatType.INT_TYPE, parRegionMonitor);

    putLocalRate = new StatsRate(StatsKey.PUT_LOCAL, StatType.INT_TYPE, parRegionMonitor);
    
    remotePutAvgLatency = new StatsAverageLatency(StatsKey.REMOTE_PUTS, StatType.INT_TYPE, StatsKey.REMOTE_PUT_TIME,
        parRegionMonitor);

    putRemoteLatency = new StatsLatency(StatsKey.REMOTE_PUTS, StatType.INT_TYPE, StatsKey.REMOTE_PUT_TIME,
        parRegionMonitor);
    
    String[] writesRates = new String[] { StatsKey.PUTALL_COMPLETED, StatsKey.PUTS_COMPLETED, StatsKey.CREATES_COMPLETED };
    averageWritesRate = new StatsRate(writesRates, StatType.INT_TYPE, parRegionMonitor);
    averageReadsRate = new StatsRate(StatsKey.GETS_COMPLETED, StatType.INT_TYPE, parRegionMonitor);
  }
  
  @Override
  public float getAverageReads() {
    return averageReadsRate.getRate();
  }

  @Override
  public float getAverageWrites() {
    return averageWritesRate.getRate();
  }
  
  @Override
  public float getCreatesRate() {
    return createsRate.getRate();
  }
  
  @Override
  public float getPutAllRate() {
    return putAllRate.getRate();
  }

  @Override
  public float getPutsRate() {
    return putRequestRate.getRate();
  }

  @Override
  public float getDestroyRate() {
    return destroysRate.getRate();
  }

  @Override
  public float getGetsRate() {
    return getRequestRate.getRate();
  }

  
  @Override
  public int getActualRedundancy() {
    return getPrStatistic(StatsKey.ACTUAL_REDUNDANT_COPIES).intValue();
  }

  @Override
  public int getAvgBucketSize() {
    return ManagementConstants.NOT_AVAILABLE_INT;
  }

  @Override
  public int getBucketCount() {
    return getPrStatistic(StatsKey.BUCKET_COUNT).intValue();
  }

  @Override
  public int getConfiguredRedundancy() {
    return configuredRedundancy;
  }

  @Override
  public int getNumBucketsWithoutRedundancy() {
    return getPrStatistic(StatsKey.LOW_REDUNDANCYBUCKET_COUNT).intValue();
  }

  @Override
  public int getPrimaryBucketCount() {
    return getPrStatistic(StatsKey.PRIMARY_BUCKET_COUNT).intValue();
  }

  @Override
  public int getTotalBucketSize() {
    return getPrStatistic(StatsKey.TOTAL_BUCKET_SIZE).intValue();
  }
  
  @Override
  public PartitionAttributesData listPartitionAttributes() {
    return partitionAttributesData;
  }

  @Override
  public FixedPartitionAttributesData[] listFixedPartitionAttributes() {    
    return fixedPartitionAttributesTable;
  }

  @Override
  public long getEntrySize() {
    if (parRegion.isDataStore()) {
      return getPrStatistic(StatsKey.DATA_STORE_BYTES_IN_USE).longValue();
    } else {
      return  ManagementConstants.ZERO;
    }
  }

  @Override
  public long getHitCount() {
    return ManagementConstants.NOT_AVAILABLE_LONG;
  }

  @Override
  public float getHitRatio() {
    return ManagementConstants.NOT_AVAILABLE_FLOAT;
  }

  @Override
  public long getLastAccessedTime() {
    return ManagementConstants.NOT_AVAILABLE_LONG;
  }
  
  @Override
  public long getLastModifiedTime() {
    return ManagementConstants.NOT_AVAILABLE_LONG;
  }
  
  
  @Override
  public long getPutRemoteAvgLatency() {
    return remotePutAvgLatency.getAverageLatency();
  }
  @Override
  public long getPutRemoteLatency() {
    return putRemoteLatency.getLatency();
  }
  
  @Override
  public float getPutLocalRate() {
    return putLocalRate.getRate();
  }

  @Override
  public float getPutRemoteRate() {
    return putRemoteRate.getRate();
  }

  /**
   * partition region entry count is taken from all primary bucket entry count.
   * Ideally it should come from stats. 
   * to be done in 8.0
   * @return long
   */
  @Override
  public long getEntryCount() {
    if (parRegion.isDataStore()) {
      int numLocalEntries = 0;
      Set<BucketRegion> localPrimaryBucketRegions = parRegion.getDataStore().getAllLocalPrimaryBucketRegions();
      if (localPrimaryBucketRegions != null && localPrimaryBucketRegions.size() > 0) {
        for (BucketRegion br : localPrimaryBucketRegions) {
          // TODO soplog, fix this for griddb regions
          numLocalEntries += br.getRegionMap().sizeInVM() - br.getTombstoneCount();

        }
      }
      return numLocalEntries;
    } else {
      return  ManagementConstants.ZERO;
    }
  }
  
  public int getLocalMaxMemory() {
    return partitionAttributesData.getLocalMaxMemory();
  }

  public long getEstimatedSizeForHDFSRegion() {
    return -1;
  }
}
