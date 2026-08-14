UPDATE asset
SET attributes = (
    SELECT jsonb_object_agg(key, CASE
        WHEN value -> 'meta' -> 'agentLink' ->> 'type' = 'SimulatorAgentLink'
        AND NOT (value -> 'meta' -> 'agentLink' ? 'schedule')
        THEN jsonb_set(value, '{meta, agentLink, schedule}',
            jsonb_build_object(
                -- Start of the day the migration ran
                'start', (floor(extract(epoch from now()) * 1000 / 86400000) * 86400000)::bigint,
                'recurrence', 'FREQ=DAILY'
            )
        ) ELSE value
    END) FROM jsonb_each(attributes)
) WHERE attributes @? '$.* ? (@.meta.agentLink.type == "SimulatorAgentLink" && !exists(@.meta.agentLink.schedule))';

-- ##### Use the following to check whether all demo setup SimulatorAgentLinks have a schedule after migrating #####
--
-- SELECT
--     COUNT(*) FILTER (WHERE value -> 'meta' -> 'agentLink' ? 'schedule') AS with_schedule,
--     COUNT(*) FILTER (WHERE NOT (value -> 'meta' -> 'agentLink' ? 'schedule')) AS without_schedule,
--     COUNT(*) AS total_simulators
-- FROM asset, jsonb_each(attributes)
-- WHERE value -> 'meta' -> 'agentLink' ->> 'type' = 'SimulatorAgentLink';

-- ##### Use the following to check the schedules that are present on SimulatorAgentLinks #####
--
-- SELECT
--     id AS asset_id,
--     key AS attribute_name,
--     value->'meta'->'agentLink'->'schedule' AS schedule_data
-- FROM asset,
--     jsonb_each(attributes)
-- WHERE value @? '$.meta.agentLink.type == "SimulatorAgentLink"'
--   AND value->'meta'->'agentLink' ? 'schedule';
