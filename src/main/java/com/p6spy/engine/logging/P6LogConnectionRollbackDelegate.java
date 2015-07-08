/*
 * #%L
 * P6Spy
 * %%
 * Copyright (C) 2013 P6Spy
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.p6spy.engine.logging;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.P6LogQuery;
import com.p6spy.engine.common.StopWatch;
import com.p6spy.engine.event.JDBCRollbackEvent;
import com.p6spy.engine.proxy.Delegate;
import com.p6spy.engine.spy.P6ModuleManager;

import java.lang.reflect.Method;

/**
 */
class P6LogConnectionRollbackDelegate implements Delegate {


  private final ConnectionInformation connectionInformation;

  public P6LogConnectionRollbackDelegate(final ConnectionInformation connectionInformation) {
    this.connectionInformation = connectionInformation;
  }

  @Override
  public Object invoke(final Object proxy, final Object underlying, final Method method, final Object[] args) throws Throwable {
    StopWatch stopWatch = new StopWatch().start();
    Throwable thrownException = null;

    try {
      return method.invoke(underlying, args);
    } catch (Throwable t) {
      thrownException = t;
      throw t;
    } finally {
      P6ModuleManager.getInstance().getEventPublisher().publish(
        new JDBCRollbackEvent(underlying)
          .withConnectionId(connectionInformation.getConnectionId())
          .withStopWatch(stopWatch)
          .withThrownException(thrownException)
      );
      P6LogQuery.logElapsed(connectionInformation.getConnectionId(), stopWatch.startTimeMs(), Category.ROLLBACK, "", "");
    }
  }
}
