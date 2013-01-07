package hrider.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

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
public class ConnectionDetails {

    //region Variables
    private ServerDetails hbaseServer;
    private ServerDetails zookeeperServer;
    //endregion

    //region Public Properties
    public ServerDetails getHbaseServer() {
        return this.hbaseServer;
    }

    public void setHbaseServer(ServerDetails hbaseServer) {
        this.hbaseServer = hbaseServer;
    }

    public ServerDetails getZookeeperServer() {
        return this.zookeeperServer;
    }

    public void setZookeeperServer(ServerDetails zookeeperServer) {
        this.zookeeperServer = zookeeperServer;
    }
    //endregion

    //region Public Methods
    public Configuration createConfig() {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", this.zookeeperServer.getHost());
        config.set("hbase.zookeeper.property.clientPort", this.zookeeperServer.getPort());
        config.set("hbase.master", this.hbaseServer.getHost() + ':' + this.hbaseServer.getPort());

        return config;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectionDetails) {
            ConnectionDetails details = (ConnectionDetails)obj;
            return this.hbaseServer.equals(details.hbaseServer) && this.zookeeperServer.equals(details.zookeeperServer);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.hbaseServer.hashCode() ^ this.zookeeperServer.hashCode();
    }
    //endregion
}
