/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

ALTER TABLE ${schemaName}.asset_datapoint SET (
    timescaledb.enable_columnstore = true,
    timescaledb.orderby = 'timestamp DESC',
    timescaledb.segmentby = 'entity_id,attribute_name');

ALTER TABLE ${schemaName}.asset_predicted_datapoint SET (
    timescaledb.enable_columnstore = true,
    timescaledb.orderby = 'timestamp DESC',
    timescaledb.segmentby = 'entity_id,attribute_name');

CALL public.add_columnstore_policy('asset_datapoint', after => INTERVAL ${columnStorePolicyInterval});
CALL public.add_columnstore_policy('asset_predicted_datapoint', after => INTERVAL ${columnStorePolicyInterval});

