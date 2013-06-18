package hrider.config;

import hrider.actions.Action;
import hrider.actions.RunnableAction;
import hrider.hbase.ConnectionManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.io.IOException;
import java.io.Serializable;

/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Igor Cher
 * @version %I%, %G%
 *          <p/>
 *          This class represents a data to be used to connect to the hbase and zookeeper nodes.
 */
public class ConnectionDetails implements Serializable {

    //region Constants
    private static final long serialVersionUID = -5808921673890223877L;
    //endregion

    //region Variables
    private ServerDetails zookeeper;
    //endregion

    //region Public Properties
    public ServerDetails getZookeeper() {
        return this.zookeeper;
    }

    public void setZookeeper(ServerDetails zookeeper) {
        this.zookeeper = zookeeper;
    }
    //endregion

    //region Public Methods
    public boolean canConnect() {
        Boolean result = RunnableAction.runAndWait(
            this.zookeeper.getHost(), new Action<Boolean>() {

            @Override
            public Boolean run() throws IOException {
                ConnectionManager.create(ConnectionDetails.this);
                return true;
            }
        }, GlobalConfig.instance().getConnectionCheckTimeout());

        return result != null ? result : false;
    }

    public Configuration createConfig() {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", this.zookeeper.getHost());
        config.set("hbase.zookeeper.property.clientPort", this.zookeeper.getPort());
        config.set("hbase.client.retries.number", "3");

//        config.set("hbase.security.authentication", "kerberos");
//        config.set("hbase.rpc.engine", "org.apache.hadoop.hbase.ipc.SecureRpcEngine");

        return config;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectionDetails) {
            ConnectionDetails details = (ConnectionDetails)obj;
            return this.zookeeper.equals(details.zookeeper);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.zookeeper.hashCode();
    }
    //endregion
}
