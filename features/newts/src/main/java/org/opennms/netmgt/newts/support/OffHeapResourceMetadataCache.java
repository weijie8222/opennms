/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.newts.support;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.List;
import java.util.stream.Collectors;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.cassandra.search.ResourceMetadata;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 * <p>
 * This will use {@code DirectByteBuffer} outside of HEAP, so Garbage Collector is not affected
 * You should increase amount of direct memory with
 * {@code -XX:MaxDirectMemorySize=10G} JVM param
 * </p>
 */
public class OffHeapResourceMetadataCache implements SearchableResourceMetadataCache {

    private static final ResourceMetadataSerializer RESOURCE_METADATA_SERIALIZER = new ResourceMetadataSerializer();

    private static final Joiner s_keyJoiner = Joiner.on(':');

    private final BTreeMap<byte[], ResourceMetadata> m_map;

    private final Meter m_metricReqs;
    private final Meter m_attributeReqs;
    private final Meter m_metricMisses;
    private final Meter m_attributeMisses;

    public OffHeapResourceMetadataCache(MetricRegistry registry) {
        final DB db = DBMaker.memoryDirectDB().make();
        m_map = db.treeMap("map")
                .keySerializer(Serializer.BYTE_ARRAY)
                .valueSerializer(RESOURCE_METADATA_SERIALIZER)
                .createOrOpen();

        m_metricReqs = registry.meter(name("cache", "metric-reqs"));
        m_metricMisses = registry.meter(name("cache", "metric-misses"));
        m_attributeReqs = registry.meter(name("cache", "attribute-reqs"));
        m_attributeMisses = registry.meter(name("cache", "attribute-misses"));

        registry.register(MetricRegistry.name("cache", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        // By default, a BTreeMap does not keep track of its size
                        // and calling map.size() requires a linear scan to count all entries.
                        // If you enable size counter, in that case map.size() is instant,
                        // but there is some overhead on the inserts.
                        return m_map.sizeLong();
                    }
                });
        registry.register(MetricRegistry.name("cache", "max-size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return 0L;
                    }
                });
    }

    @Override
    public void merge(Context context, Resource resource, ResourceMetadata metadata) {
        final Optional<ResourceMetadata> o = get(context, resource);

        if (!o.isPresent()) {
            final ResourceMetadata newMetadata = new ResourceMetadata(m_metricReqs, m_attributeReqs, m_metricMisses, m_attributeMisses);
            newMetadata.merge(metadata);
            m_map.put(key(context, resource.getId()), newMetadata);
            return;
        }

        o.get().merge(metadata);
    }

    @Override
    public Optional<ResourceMetadata> get(Context context, Resource resource) {
        final ResourceMetadata resourceMetadata = m_map.get(key(context, resource.getId()));
        return Optional.fromNullable(resourceMetadata);
    }

    @Override
    public void delete(Context context, Resource resource) {
        m_map.remove(key(context, resource.getId()));
    }

    @Override
    public List<String> getResourceIdsWithPrefix(Context context, String resourceIdPrefix) {
        return m_map.prefixSubMap(key(context, resourceIdPrefix)).keySet().stream()
                .map(b -> resourceId(context, b))
                .collect(Collectors.toList());
    }

    private byte[] key(Context context, String resourceId) {
        return s_keyJoiner.join(context.getId(), resourceId).getBytes();
    }

    private String resourceId(Context context, byte[] key) {
        final int numBytesForContext = context.getId().getBytes().length;
        return new String(key, numBytesForContext + 1, key.length - numBytesForContext - 1);
    }
}
