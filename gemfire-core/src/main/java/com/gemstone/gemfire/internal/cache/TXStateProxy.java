/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/**
 * File comment
 */
package com.gemstone.gemfire.internal.cache;

import com.gemstone.gemfire.cache.client.internal.ServerRegionDataAccess;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.internal.cache.tx.TransactionalOperation.ServerRegionOperation;

/**
 * This interface extends {@link TXStateInterface} providing for a proxy for the
 * real transaction on a remote data store node.
 * 
 * @author mthomas
 */
public interface TXStateProxy extends TXStateInterface {

  public void checkJTA(String errmsg) throws IllegalStateException;
  
  public void setIsJTA(boolean isJTA);

  public TXId getTxId();

  public TXManagerImpl getTxMgr();

  public void setLocalTXState(TXStateInterface state);

  public void setTarget(DistributedMember target);

  public DistributedMember getTarget();
  
  public boolean isCommitOnBehalfOfRemoteStub();
  
  public boolean setCommitOnBehalfOfRemoteStub(boolean requestedByOwner);
  
  public boolean isOnBehalfOfClient();
  
  public boolean isJCATransaction();
  public void setJCATransaction();
  
  /**
   * establishes the synchronization thread used for client/server
   * beforeCompletion/afterCompletion processing
   * @param sync
   */
  public void setSynchronizationRunnable(TXSynchronizationRunnable sync);
  public TXSynchronizationRunnable getSynchronizationRunnable();
  
  /**
   * Called by {@link TXManagerImpl#internalSuspend()} to perform additional
   * tasks required to suspend a transaction
   */
  public void suspend();
  
  /**
   * Called by {@link TXManagerImpl#resume(TXStateProxy)} to
   * perform additional tasks required to resume a transaction
   */
  public void resume();
  
  /**
   * record a client-side transactional operation for possible later replay
   */
  public void recordTXOperation(ServerRegionDataAccess proxy, ServerRegionOperation op, Object key, Object[] arguments);
  
  /**
   * @return the number of operations performed in this transaction
   */
  public int operationCount();
  
  /**
   * During client transaction failover, it is possible
   * to get two Commit (rollback) requests for a single transaction.
   * It becomes necessary to set the progress flag when the second
   * request arrives. When the requeset is processed, progress flag
   * must be reset. see bug 43350
   * @param progress
   */
  public void setInProgress(boolean progress);
}
