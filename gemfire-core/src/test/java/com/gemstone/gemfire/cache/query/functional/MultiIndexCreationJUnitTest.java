package com.gemstone.gemfire.cache.query.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.cache.query.CacheUtils;
import com.gemstone.gemfire.cache.query.Index;
import com.gemstone.gemfire.cache.query.IndexExistsException;
import com.gemstone.gemfire.cache.query.MultiIndexCreationException;
import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.cache.query.data.Portfolio;
import com.gemstone.gemfire.cache.query.internal.QueryObserver;
import com.gemstone.gemfire.cache.query.internal.QueryObserverAdapter;
import com.gemstone.gemfire.cache.query.internal.QueryObserverHolder;
import com.gemstone.gemfire.cache.query.internal.index.CompactRangeIndex;
import com.gemstone.gemfire.cache.query.internal.index.HashIndex;
import com.gemstone.gemfire.cache.query.internal.index.PrimaryKeyIndex;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class MultiIndexCreationJUnitTest {
  private static String regionName = "portfolios";
  private String prRegionName = "prPortfolios";
  private String overflowRegionName = "overflowPortfolios";
  
  @Before
  public void setUp() throws java.lang.Exception {
    CacheUtils.startCache();
    Region region = CacheUtils.createRegion(regionName, Portfolio.class);
  }

  @After
  public void tearDown() throws java.lang.Exception {
    CacheUtils.closeCache();
  }

  @Test
  public void testBasicMultiIndexCreation() throws Exception {
    Region r = CacheUtils.getRegion(regionName);
    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }

    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("statusIndex", "status", r.getFullPath());
    qs.defineIndex("IDIndex", "ID", r.getFullPath());
    List<Index> indexes = qs.createDefinedIndexes();
    
    assertEquals("Only 2 indexes should have been created. ", 2, indexes.size());
    
    Index ind = qs.getIndex(r, "statusIndex");
    assertEquals(2, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(r, "IDIndex");
    assertEquals(10, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    QueryObserver old = QueryObserverHolder
        .setInstance(new QueryObserverAdapter() {
          private boolean indexCalled = false;

          public void afterIndexLookup(Collection results) {
            indexCalled = true;
          }

          public void endQuery() {
            assertTrue(indexCalled);
          }

        });
    
    String[] queries = {
        "select * from " + r.getFullPath() + " where status = 'active'",
        "select * from " + r.getFullPath() + " where ID > 4"
        };
    
    for(int i = 0 ; i < queries.length; i++) {
      SelectResults sr = (SelectResults) qs.newQuery(queries[i]).execute();
      assertEquals(5, sr.size());
    }
    QueryObserverHolder.setInstance(old);
  }
  
  @Test
  public void testBasicMultiIndexCreationDifferentTypes() throws Exception {
    Region r = CacheUtils.getRegion(regionName);
    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }

    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("statusIndex", "status", r.getFullPath());
    qs.defineHashIndex("IDIndex", "ID", r.getFullPath());
    qs.defineKeyIndex("keyIDIndex", "ID", r.getFullPath());
    List<Index> indexes = qs.createDefinedIndexes();
    
    assertEquals("Only 3 indexes should have been created. ", 3, indexes.size());
    
    Index ind = qs.getIndex(r, "statusIndex");
    assertTrue(ind instanceof CompactRangeIndex);
    assertEquals(2, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(r, "IDIndex");
    assertTrue(ind instanceof HashIndex);
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(r, "keyIDIndex");
    assertTrue(ind instanceof PrimaryKeyIndex);
    assertEquals(10, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    QueryObserver old = QueryObserverHolder
        .setInstance(new QueryObserverAdapter() {
          private boolean indexCalled = false;

          public void afterIndexLookup(Collection results) {
            indexCalled = true;
          }

          public void endQuery() {
            assertTrue(indexCalled);
          }

        });
    
    String[] queries = {
        "select * from " + r.getFullPath() + " where status = 'active'",
        "select * from " + r.getFullPath() + " where ID > 4"
        };
    
    for(int i = 0 ; i < queries.length; i++) {
      SelectResults sr = (SelectResults) qs.newQuery(queries[i]).execute();
      assertEquals(5, sr.size());
    }
    QueryObserverHolder.setInstance(old);
  }
  
  
  @Test
  public void testMultiIndexCreationOnlyDefine() throws Exception {
    Region r = CacheUtils.getRegion(regionName);
    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }

    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("statusIndex", "status", r.getFullPath());
    qs.defineIndex("IDIndex", "ID", r.getFullPath());
    
    Index ind = qs.getIndex(r, "statusIndex");
    assertNull("Index should not have been created",ind);
    
    ind = qs.getIndex(r, "IDIndex");
    assertNull("Index should not have been created",ind);
    
    QueryObserver old = QueryObserverHolder
        .setInstance(new QueryObserverAdapter() {
          private boolean indexCalled = false;

          public void afterIndexLookup(Collection results) {
            indexCalled = true;
          }

          public void endQuery() {
            assertFalse(indexCalled);
          }

        });
    
    String[] queries = {
        "select * from " + r.getFullPath() + " where status = 'active'",
        "select * from " + r.getFullPath() + " where ID > 4"
        };
    
    for(int i = 0 ; i < queries.length; i++) {
      SelectResults sr = (SelectResults) qs.newQuery(queries[i]).execute();
      assertEquals(5, sr.size());
    }
    QueryObserverHolder.setInstance(old);
  }
  
  
  @Test
  public void testMultiIndexCreationOnFailure() throws Exception {
    Region r = CacheUtils.getRegion(regionName);
    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }

    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("IDIndex1", "ID", r.getFullPath());
    qs.defineIndex("IDIndex2", "ID", r.getFullPath());
    List<Index> indexes = null;
    try {
      indexes = qs.createDefinedIndexes();
      fail("Exception should have been thrown");
    } catch(MultiIndexCreationException me) {
      assertTrue("IndexExistsException should have been thrown ", me.getExceptionsMap().values().iterator().next() instanceof IndexExistsException);
    }
    assertNull("Index should not have been returned",indexes);
    
    assertEquals("1 index should have been created.", 1, qs.getIndexes().size());
   
    Index ind = qs.getIndexes().iterator().next();
    assertNotNull("Index should not be null.", ind);
    assertEquals(10, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
  }
  
  @Test
  public void testIndexCreationOnMultipleRegions() throws Exception {
    Region pr = CacheUtils.getCache().createRegionFactory(RegionShortcut.PARTITION).create(prRegionName);
    for (int i = 0; i < 10; i++) {
      pr.put("" + i, new Portfolio(i));
    }
    
    Region overflow = CacheUtils.getCache().createRegionFactory(RegionShortcut.REPLICATE_OVERFLOW).create(overflowRegionName);
    for (int i = 0; i < 10; i++) {
      overflow.put("" + i, new Portfolio(i));
    }
    
    Region r = CacheUtils.getRegion(regionName);
    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }
    
    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("IDIndex", "ID", pr.getFullPath());
    qs.defineIndex("secIDIndex", "pos.secId", r.getFullPath() + " p, p.positions.values pos ");
    qs.defineIndex("statusIndex", "status", overflow.getFullPath() );

    List<Index> indexes = qs.createDefinedIndexes();
    
    assertEquals("Only 3 indexes should have been created. ", 3, indexes.size());
    
    Index ind = qs.getIndex(overflow, "statusIndex");
    assertEquals(2, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(pr, "IDIndex");
    assertEquals(10, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(r, "secIDIndex");
    assertEquals(12, ind.getStatistics().getNumberOfKeys());
    assertEquals(20, ind.getStatistics().getNumberOfValues());
    
    QueryObserver old = QueryObserverHolder
        .setInstance(new QueryObserverAdapter() {
          private boolean indexCalled = false;

          public void afterIndexLookup(Collection results) {
            indexCalled = true;
          }

          public void endQuery() {
            assertTrue(indexCalled);
          }

        });
    
    String[] queries = {
        "select * from " + overflow.getFullPath() + " where status = 'active'",
        "select * from " + pr.getFullPath() + " where ID > 4",
        "select * from " + r.getFullPath() + " p, p.positions.values pos where pos.secId != NULL"
        };
    
    for(int i = 0 ; i < queries.length; i++) {
      SelectResults sr = (SelectResults) qs.newQuery(queries[i]).execute();
      if(i == 2) {
        assertEquals("Incorrect results for query: " + queries[i], 20, sr.size());
      } else {
        assertEquals("Incorrect results for query: " + queries[i], 5, sr.size());
      }
    }
    QueryObserverHolder.setInstance(old);
  
  }
  
  @Test
  public void testIndexCreationOnMultipleRegionsBeforePuts() throws Exception {
    Region pr = CacheUtils.getCache().createRegionFactory(RegionShortcut.PARTITION).create(prRegionName);
    Region overflow = CacheUtils.getCache().createRegionFactory(RegionShortcut.REPLICATE_OVERFLOW).create(overflowRegionName);
    Region r = CacheUtils.getRegion(regionName);
    
    QueryService qs = CacheUtils.getQueryService();
    qs.defineIndex("IDIndex", "ID", pr.getFullPath());
    qs.defineIndex("secIDIndex", "pos.secId", r.getFullPath() + " p, p.positions.values pos ");
    qs.defineIndex("statusIndex", "status", overflow.getFullPath());
    
    List<Index> indexes = qs.createDefinedIndexes();

    for (int i = 0; i < 10; i++) {
      r.put("" + i, new Portfolio(i));
    }

    for (int i = 0; i < 10; i++) {
      pr.put("" + i, new Portfolio(i));
    }

    for (int i = 0; i < 10; i++) {
      overflow.put("" + i, new Portfolio(i));
    }
    
    assertEquals("Only 3 indexes should have been created. ", 3, indexes.size());
    
    Index ind = qs.getIndex(overflow, "statusIndex");
    assertEquals(2, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(pr, "IDIndex");
    assertEquals(10, ind.getStatistics().getNumberOfKeys());
    assertEquals(10, ind.getStatistics().getNumberOfValues());
    
    ind = qs.getIndex(r, "secIDIndex");
    assertEquals(12, ind.getStatistics().getNumberOfKeys());
    assertEquals(20, ind.getStatistics().getNumberOfValues());
    
    QueryObserver old = QueryObserverHolder
        .setInstance(new QueryObserverAdapter() {
          private boolean indexCalled = false;

          public void afterIndexLookup(Collection results) {
            indexCalled = true;
          }

          public void endQuery() {
            assertTrue(indexCalled);
          }

        });
    
    String[] queries = {
        "select * from " + overflow.getFullPath() + " where status = 'active'",
        "select * from " + pr.getFullPath() + " where ID > 4",
        "select * from " + r.getFullPath() + " p, p.positions.values pos where pos.secId != NULL"
        };
    
    for(int i = 0 ; i < queries.length; i++) {
      SelectResults sr = (SelectResults) qs.newQuery(queries[i]).execute();
      if(i == 2) {
        assertEquals("Incorrect results for query: " + queries[i], 20, sr.size());
      } else {
        assertEquals("Incorrect results for query: " + queries[i], 5, sr.size());
      }
    }
    QueryObserverHolder.setInstance(old);
  
  }
 
}
