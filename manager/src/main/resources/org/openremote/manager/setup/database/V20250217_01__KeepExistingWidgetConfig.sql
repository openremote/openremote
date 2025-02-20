/*
* Set existing widgets configs to match their pre-update behavior.
*/

WITH json_data AS (
    SELECT
        id,
        template,
        COALESCE(
                jsonb_set(
                        template,
                        '{widgets}',
                        (
                            SELECT jsonb_agg(
                                           jsonb_set(
                                                   widget,
                                                   '{widgetConfig,allOfType}',
                                                   CASE
                                                       WHEN (widget ->> 'widgetTypeId') = 'map' THEN 'true'::jsonb
                                                       WHEN (widget ->> 'widgetTypeId') = 'table' THEN 'false'::jsonb
                                                       ELSE widget -> 'widgetConfig' -> 'allOfType'
                                                       END
                                           )
                                   )
                            FROM jsonb_array_elements(template -> 'widgets') AS widget
                        )
                ),
                template
        ) AS updated_json
    FROM dashboard
)
UPDATE dashboard
SET template = json_data.updated_json
FROM json_data
WHERE dashboard.id = json_data.id;
