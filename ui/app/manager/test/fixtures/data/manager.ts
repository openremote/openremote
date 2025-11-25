export const markers = [
    {
        attributeName: "onOff",
        showLabel: false,
        showUnits: false,
        colours: {
            type: "boolean",
            true: "fce61e",
            false: "000000",
        },
    },
    {
        attributeName: "temperature",
        showLabel: true,
        showUnits: true,
        colours: {
            type: "range",
            ranges: [
                {
                    min: 0,
                    colour: "39B54A",
                },
                {
                    min: 30,
                    colour: "F7931E",
                },
                {
                    min: 40,
                    colour: "C1272D",
                },
            ],
        },
    },
] as const;
