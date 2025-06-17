UPDATE dashboard
SET template = jsonb_set(
        template,
        '{widgets}',
        (
            SELECT jsonb_agg(
                           CASE
                               WHEN widget->>'widgetTypeId' = 'gateway' THEN
                                   jsonb_set(widget, '{widgetConfig,attributeRefs}', ('[' || widget->'widgetConfig'->>'gatewayId' || ']')::jsonb)
                               ELSE
                                   widget  -- Leave non-gateway widgets unchanged
                               END
                   )
            FROM jsonb_array_elements(template->'widgets') widget
        )
)
WHERE template->'widgets' IS NOT NULL;
