/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/*
 * IndependentOperandsInWhereClause.java
 *
 * Created on June 23, 2005, 4:24 PM
 */
package com.gemstone.gemfire.cache.query.functional;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.query.CacheUtils;
import com.gemstone.gemfire.cache.query.Index;
import com.gemstone.gemfire.cache.query.IndexType;
import com.gemstone.gemfire.cache.query.Query;
import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.cache.query.Utils;
import com.gemstone.gemfire.cache.query.data.Portfolio;
import com.gemstone.gemfire.cache.query.internal.QueryObserverAdapter;
import com.gemstone.gemfire.cache.query.internal.QueryObserverHolder;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 *
 * @author kdeshpan
 */
@Category(IntegrationTest.class)
public class IndependentOperandsInWhereClause2JUnitTest {
    
    
  @Before
  public void setUp() throws Exception {
        CacheUtils.startCache();
    }
    
  @After
  public void tearDown() throws Exception {
        CacheUtils.closeCache();
    }
    
  @Test
  public void testIndependentOperands() throws Exception {
        Region region = CacheUtils.createRegion("portfolios", Portfolio.class);
        for (int i = 0; i < 4; i++) {
            region.put("" + i, new Portfolio(i));
        }
        QueryService qs = CacheUtils.getQueryService();
        String queries[] = {
         /*1*/  "select distinct * from /portfolios pf, positions.values pos where  (pf.ID > 1 or status = 'active') or (true AND pos.secId ='IBM')",// size 6 noi 3
         /*2*/  "Select distinct structset.sos, structset.key " +
                "from /portfolios pfos, pfos.positions.values outerPos, " +
                "(SELECT DISTINCT key: key, sos: pos.sharesOutstanding "+
                "from /portfolios.entries pf, pf.value.positions.values pos " +
                "where outerPos.secId != 'IBM' AND " +
                "pf.key IN (select distinct * from pf.value.collectionHolderMap['0'].arr)) structset " +
                "where structset.sos > 2000",// size 6 numof indexes 0.
         /*3*/  "Select distinct * " +
                "from /portfolios pfos, pfos.positions.values outerPos, " +
                "(SELECT DISTINCT key: key, sos: pos.sharesOutstanding "+
                "from /portfolios.entries pf, pf.value.positions.values pos " +
                "where outerPos.secId != 'IBM' AND " +
                "pf.key IN (select distinct * from pf.value.collectionHolderMap['0'].arr)) structset " +
                "where structset.sos > 2000",// size 42 numof indexes 0.
         /*4*/  "select distinct * from /portfolios pf where true and ID = 0 ",//size 1, noi 1
         /*5*/  "select distinct * from /portfolios pf, positions.values pos where true = true and pos.secId ='IBM'",//size 1 noi 1
         /*6*/  "select distinct * from /portfolios pf, positions.values pos where false and pos.secId ='IBM'",//size 0 noi
         /*7*/  "select distinct * from /portfolios pf where true or ID = 0 ",//size 4
         /*8*/  "select distinct * from /portfolios pf, positions.values pos  where true = true and pf.ID > 1 and pos.secId ='IBM'",//size 0
         /*9*/  "select distinct * from /portfolios pf, positions.values pos where true = false and pf.ID > 1 and pos.secId ='IBM'",//size 0
         /*10*/ "select distinct * from /portfolios pf, positions.values pos where true = true and pf.ID > 1 or pos.secId ='IBM'",//size 5
         /*11*/ "select distinct * from /portfolios pf, positions.values pos where true = false and pf.ID > 1 or pos.secId ='IBM'",//size 1
         /*12*/ "select distinct * from /portfolios pf, positions.values pos where  (true AND pos.secId ='SUN') or (pf.ID > 1 and status != 'active')",//size 2
         /*13*/ "select distinct * from /portfolios pf, positions.values pos  where  (pf.ID > 1 or status = 'active') or (false AND pos.secId ='IBM')",//size 6
         /*14*/ "SELECT DISTINCT * FROM /portfolios pf, pf.positions.values position "+
         "WHERE true = null OR position.secId = 'SUN'", // size 1
         /*15*/ "SELECT DISTINCT * FROM /portfolios pf, pf.positions.values position "+
         "WHERE (true = null OR position.secId = 'SUN') AND true", //size 1
         /*16*/ "SELECT DISTINCT * FROM /portfolios pf, pf.positions.values position "+
         "WHERE (ID > 0 OR position.secId = 'SUN') OR false",    //size 6
         /*17*/ "SELECT DISTINCT * FROM /portfolios pf, pf.positions.values position "+
         "WHERE (true = null OR position.secId = 'SUN') OR true",         //size 8
         /*18*/ "SELECT DISTINCT * FROM /portfolios pf, pf.positions.values position "+
         "WHERE (true = null OR position.secId = 'SUN') OR false",      //size 1
         /*19*/ "select distinct * from /portfolios pf, positions.values pos " +
         "where (pf.ID < 1 and status = 'active') and (false or pos.secId = 'IBM')",// size 1
         /*20*/ "select distinct * from /portfolios pf where false and ID = 0 ",// size 0
         /*21*/ "select distinct * from /portfolios pf where false or ID = 0 ",// size 1
         /*22*/ "select distinct * from /portfolios pf where ID = 0 and false  ",// size 0
         /*23*/ "select distinct * from /portfolios pf where ID = 0 or false" ,// size 1
         /*24*/ "select distinct * from /portfolios pf, positions.values pos " +
         "where (ID = 2 and true) and (status = 'active' or (pos.secId != 'IBM' and true))  ",// size 2
         /*25*/ "select distinct * from /portfolios pf, positions.values pos " +
         "where (ID = 2 or false) or (status = 'active' and (pos.secId != 'IBM' or true))  ",// size 2
         /*26*/ "SELECT DISTINCT * FROM /portfolios pf,"
         + " (SELECT DISTINCT * FROM /portfolios ptf, ptf.positions pos where pf.ID != 1 and pos.value.sharesOutstanding > 2000) as x"
         + " WHERE pos.value.secId = 'IBM'", //size 0
         /*27*/ "SELECT DISTINCT * FROM /portfolios pf,"
         + " (SELECT DISTINCT * FROM /portfolios ptf, ptf.positions pos where pf.ID != 1 and pos.value.sharesOutstanding > 2000)as y"
         + " WHERE pos.value.secId = 'HP'", //size 3
        };
        
        int sizeOfResult[] = {6,6,42,1,1,0,4,0,0,5,1,2,6,1,1,6,8,1,1,0,1,0,1,2,4,0,3};
        SelectResults sr[][]= new SelectResults[queries.length][2];
        for (int i = 0; i < queries.length; i++) {
            Query q = null;
            q = CacheUtils.getQueryService().newQuery(queries[i]);
            QueryObserverImpl observer = new QueryObserverImpl();
            QueryObserverHolder.setInstance(observer);
            
            Object r = q.execute();
            sr[i][0] = (SelectResults) r;
            if (!observer.isIndexesUsed) {
                CacheUtils.log("NO INDEX USED");
            }
            CacheUtils.log(Utils.printResult(r));
            if(((Collection)r).size() != sizeOfResult[i]) {
                fail("SIZE NOT as expected for QUery no :" + (i+1));
            }
        }
        
        //Create an Index and Run the Same Query as above.
        //        Index index1 = (Index) qs.createIndex("secIdIndex", IndexType.FUNCTIONAL,
        //                "b.secId", "/portfolios.entries pf, pf.value.positions.values b");
        qs.createIndex("secIdIdx", IndexType.FUNCTIONAL,
                "b.secId", "/portfolios pf, pf.positions.values b");
        qs.createIndex("IdIdx", IndexType.FUNCTIONAL,
                "pf.ID", "/portfolios pf, pf.positions.values b");
        qs.createIndex("statusIdx", IndexType.FUNCTIONAL,
                "pf.status", "/portfolios pf, pf.positions.values b");
        qs.createIndex("Idindex2", IndexType.FUNCTIONAL, "pf.ID","/portfolios pf");
        for (int j = 0; j < queries.length; j++) {
            Query q2 = null;
            q2 = CacheUtils.getQueryService().newQuery(queries[j]);
            QueryObserverImpl observer2 = new QueryObserverImpl();
            QueryObserverHolder.setInstance(observer2);
            try {
            
                Object r2 = q2.execute();
                sr[j][1] = (SelectResults) r2;
                CacheUtils.log("With Index ="+Utils.printResult(r2));
                if(((Collection)r2).size() != sizeOfResult[j]) {
                    fail("SIZE NOT as expected for QUery no :" + (j+1));
                }
            } catch(Exception e) {
                CacheUtils.log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!CAUGHT EXCPETION AT QUERY NO: " + (j+1));
                e.printStackTrace();
                fail();
            }
            //        if (observer2.isIndexesUsed == true)
            //            CacheUtils.log("YES,INDEX IS USED!!");
            //        else {
            //            fail("FAILED: Index NOT Used");
            //        }
            //            CacheUtils.log(Utils.printResult(r2));
        }
        CacheUtils.compareResultsOfWithAndWithoutIndex(sr, this);
        
        
    }
    
    class QueryObserverImpl extends QueryObserverAdapter {
        
        boolean isIndexesUsed = false;
        ArrayList indexesUsed = new ArrayList();
        
        public void beforeIndexLookup(Index index, int oper, Object key) {
            indexesUsed.add(index.getName());
        }
        
        public void afterIndexLookup(Collection results) {
            if (results != null) {
                isIndexesUsed = true;
            }
        }
    }
}