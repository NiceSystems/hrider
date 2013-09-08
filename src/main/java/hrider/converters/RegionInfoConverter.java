package hrider.converters;

import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.*;

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
 *          This class is a place holder for RegionInfo editors.
 */
public class RegionInfoConverter extends JsonConverter {

    private static class RegionInfoWrapper {

        private String  tableName;
        private String  startKey;
        private String  endKey;
        private boolean offLine;
        private boolean split;
        private long    regionID;

        RegionInfoWrapper(HRegionInfo info) {
            tableName = Bytes.toStringBinary(info.getTableName());
            startKey = Bytes.toStringBinary(info.getStartKey());
            endKey = Bytes.toStringBinary(info.getEndKey());
            offLine = info.isOffline();
            split = info.isSplit();
            regionID = info.getRegionId();
        }

        public HRegionInfo toHRegion() {
            HRegionInfo info = new HRegionInfo(
                Bytes.toBytesBinary(tableName), Bytes.toBytesBinary(startKey), Bytes.toBytesBinary(endKey), split, regionID);

            info.setOffline(offLine);
            return info;
        }
    }

    private static final long serialVersionUID = -3479231987593431384L;

    @Override
    public byte[] toBytes(String value) {
        if (value == null) {
            return EMPTY_BYTES_ARRAY;
        }

        RegionInfoWrapper wrapper = fromJson(value, RegionInfoWrapper.class);
        if (wrapper != null) {
            try {
                HRegionInfo info = wrapper.toHRegion();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                DataOutput output = new DataOutputStream(stream);
                info.write(output);

                return stream.toByteArray();
            }
            catch (IOException e) {
                logger.error(e, "Failed to convert HRegionInfo to byte array.");
            }
        }

        return Bytes.toBytes(value);
    }

    @Override
    public String toString(byte[] value) {
        if (value == null) {
            return null;
        }

        try {
            HRegionInfo info = new HRegionInfo();
            info.readFields(new DataInputStream(new ByteArrayInputStream(value)));

            return toJson(new RegionInfoWrapper(info));
        }
        catch (IOException e) {
            logger.error(e, "Failed to convert byte array to HRegionInfo.");
            return Bytes.toString(value);
        }
    }
}
