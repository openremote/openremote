UPDATE dashboard
SET template = jsonb_set(
        template,
        '{widgets}',
        (SELECT jsonb_agg(
                        CASE
                            WHEN widget ->> 'widgetTypeId' = 'gateway' AND
                                 widget -> 'widgetConfig' ->> 'gatewayId' <> '' THEN
                                jsonb_set(
                                        widget,
                                        '{widgetConfig,attributeRefs}',
                                        jsonb_build_array(
                                                jsonb_build_object(
                                                        'id', widget -> 'widgetConfig' ->> 'gatewayId',
                                                        'name', NULL
                                                )
                                        )
                                )
                            ELSE
                                widget -- Leave non-gateway widgets unchanged
                            END
                )
         FROM jsonb_array_elements(template -> 'widgets') widget)
               )
WHERE template -> 'widgets' IS NOT NULL;
