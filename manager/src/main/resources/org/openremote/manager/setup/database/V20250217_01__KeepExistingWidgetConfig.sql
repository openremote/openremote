UPDATE dashboard
SET template = jsonb_set(
        template,
        '{widgets}',
        (
            SELECT jsonb_agg(
                           CASE
                               WHEN widget->>'widgetTypeId' = 'map' THEN
                                   jsonb_set(widget, '{widgetConfig,allOfType}', 'true'::jsonb)
                               WHEN widget->>'widgetTypeId' = 'table' THEN
                                   jsonb_set(widget, '{widgetConfig,allOfType}', 'false'::jsonb)
                               ELSE
                                   widget  -- Leave non-map/table widgets unchanged
                               END
                   )
            FROM jsonb_array_elements(template->'widgets') widget
        )
)
WHERE template->'widgets' IS NOT NULL;
