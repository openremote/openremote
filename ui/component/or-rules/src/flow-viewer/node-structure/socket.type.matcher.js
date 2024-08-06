// TODO this should be defined in the back end
export class SocketTypeMatcher {
    static match(a, b) {
        return a === "ANY" /* NodeDataType.ANY */ ||
            b === "ANY" /* NodeDataType.ANY */ ||
            SocketTypeMatcher.matches.find((t) => t.type === a).matches.includes(b);
    }
}
SocketTypeMatcher.matches = [
    {
        type: "NUMBER" /* NodeDataType.NUMBER */,
        matches: [
            "NUMBER" /* NodeDataType.NUMBER */,
            "STRING" /* NodeDataType.STRING */,
        ]
    },
    {
        type: "STRING" /* NodeDataType.STRING */,
        matches: [
            "STRING" /* NodeDataType.STRING */,
        ]
    },
    {
        type: "TRIGGER" /* NodeDataType.TRIGGER */,
        matches: [
            "TRIGGER" /* NodeDataType.TRIGGER */,
        ]
    },
    {
        type: "BOOLEAN" /* NodeDataType.BOOLEAN */,
        matches: [
            "BOOLEAN" /* NodeDataType.BOOLEAN */,
            "STRING" /* NodeDataType.STRING */,
        ]
    },
    {
        type: "COLOR" /* NodeDataType.COLOR */,
        matches: [
            "COLOR" /* NodeDataType.COLOR */,
            "STRING" /* NodeDataType.STRING */,
        ]
    },
];
//# sourceMappingURL=socket.type.matcher.js.map