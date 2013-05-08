package hrider.config;

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
 *          This class represents a details of the server to connect to..
 */
public class ServerDetails implements Serializable {

    //region Constants
    private static final long serialVersionUID = -407779299748851910L;
    //endregion

    //region Variables
    /**
     * The name of the server.
     */
    private String host;
    /**
     * The port.
     */
    private String port;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link ServerDetails} class.
     *
     * @param host The server name.
     * @param port The server port.
     */
    public ServerDetails(String host, String port) {
        this.host = host;
        this.port = port;
    }
    //endregion

    //region Public Properties

    /**
     * Gets the name of the server.
     *
     * @return The name of the server.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Sets a new server name.
     *
     * @param host A new server name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets a port.
     *
     * @return An {@link Integer} representing the port.
     */
    public String getPort() {
        return this.port;
    }

    /**
     * Sets a new port.
     *
     * @param port A new port.
     */
    public void setPort(String port) {
        this.port = port;
    }
    //endregion

    //region Public Methods
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServerDetails) {
            ServerDetails details = (ServerDetails)obj;
            return this.host.equals(details.host);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.host.hashCode();
    }
    //endregion
}