UPDATE dashboard
SET template = jsonb_set(
        template,
        '{widgets}',
        (SELECT jsonb_agg(
                        CASE
                            WHEN widget ->> 'widgetTypeId' = 'gateway' THEN
                                jsonb_set(
                                        widget,
                                        '{widgetConfig,attributeRefs}',
                                        CASE
                                            WHEN widget -> 'widgetConfig' ->> 'gatewayId' != '' THEN
                                                jsonb_build_array(
                                                        jsonb_build_object(
                                                                'id', widget -> 'widgetConfig' ->> 'gatewayId',
                                                                'name', NULL
                                                        )
                                                )
                                            ELSE
                                                '[]'::jsonb -- Set to an empty array if gatewayId is empty
                                            END
                                )
                            ELSE
                                widget -- Leave non-gateway widgets unchanged
                            END
                )
         FROM jsonb_array_elements(template -> 'widgets') widget)
               )
WHERE template -> 'widgets' IS NOT NULL;
