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
                    colour: "39b54a",
                },
                {
                    min: 30,
                    colour: "f7931e",
                },
                {
                    min: 40,
                    colour: "c1272d",
                },
            ],
        },
    },
] as const;
