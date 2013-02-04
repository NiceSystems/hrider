package hrider.config;

import java.util.Collection;

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
 *          This class is responsible for saving/retrieving configuration properties related to the full view.
 */
public class ViewConfig extends PropertiesConfig {

    //region Variables
    private static ViewConfig instance;
    //endregion

    //region Constructor
    static {
        instance = new ViewConfig();
    }

    /**
     * Initializes a new instance of the {@link ViewConfig} class.
     */
    public ViewConfig() {
        super("view");
    }
    //endregion

    //region Public Methods

    /**
     * Gets the singleton instance of the {@link ViewConfig} class.
     *
     * @return A reference to the {@link ViewConfig} singleton.
     */
    public static ViewConfig instance() {
        return instance;
    }

    /**
     * Gets a list of all clusters that are open or were previously open.
     *
     * @return A list of cluster names.
     */
    public Collection<String> getClusters() {
        return getAll("cluster.").values();
    }

    /**
     * Adds a cluster to the configuration.
     *
     * @param name The name of the cluster to add.
     */
    public void addCluster(String name) {
        set(String.format("cluster.%s", name), name);
        save();
    }

    /**
     * Removes a cluster from the configuration.
     *
     * @param name The name of the cluster to remove.
     */
    public void removeCluster(String name) {
        remove(String.format("cluster.%s", name));
        save();
    }
    //endregion
}
