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

import com.p6spy.engine.common.P6LogQuery;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.common.StopWatch;
import com.p6spy.engine.event.JDBCStatementExecuteEvent;
import com.p6spy.engine.proxy.Delegate;
import com.p6spy.engine.proxy.ProxyFactory;
import com.p6spy.engine.spy.P6ModuleManager;

import java.lang.reflect.Method;
import java.sql.ResultSet;

class P6LogStatementExecuteDelegate implements Delegate {
  private final StatementInformation statementInformation;

  public P6LogStatementExecuteDelegate(final StatementInformation statementInformation) {
    this.statementInformation = statementInformation;
  }

  @Override
  public Object invoke(final Object proxy, final Object underlying, final Method method, final Object[] args) throws Throwable {
    statementInformation.setStatementQuery((String) args[0]);
    StopWatch stopWatch = new StopWatch().start();
    Throwable thrownException = null;

    try {
      Object result = method.invoke(underlying, args);
      if( result != null && result instanceof ResultSet) {
        P6LogResultSetInvocationHandler resultSetInvocationHandler = new P6LogResultSetInvocationHandler((ResultSet)result, statementInformation);
        result = ProxyFactory.createProxy((ResultSet)result, resultSetInvocationHandler);
      }
      return result;
    } catch (Throwable t) {
      thrownException = t;
      throw t;
    } finally {
      P6ModuleManager.getInstance().getEventPublisher().publish(
        new JDBCStatementExecuteEvent(underlying)
          .withConnectionId(statementInformation.getConnectionId())
          .withStopWatch(stopWatch)
          .withThrownException(thrownException)
          .withSQLWrapper(statementInformation)
      );
      P6LogQuery.logElapsed(statementInformation.getConnectionId(), stopWatch.startTimeMs(), Category.STATEMENT, statementInformation);
    }
  }
}
