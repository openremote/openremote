export const schemas = [
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "String",
    type: "string",
    "or:test:value": "test",
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Boolean",
    type: "boolean",
    "or:test:value": true,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Integer",
    type: "integer",
    "or:test:value": 1,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Number",
    type: "number",
    "or:test:value": 1,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Object",
    type: "object",
    "or:test:props": ["value"],
    properties: {
      value: {
        type: "string",
        "or:test:value": "test",
      },
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Array",
    type: "array",
    "or:test:item:count": 1,
    items: {
      title: "String",
      type: "string",
      "or:test:value": "test",
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "3 Dimensional Array",
    type: "array",
    "or:test:item:count": 2,
    items: {
      title: "2 Dimensional Array",
      type: "array",
      "or:test:item:count": 2,
      items: {
        title: "Array",
        type: "array",
        "or:test:item:count": 2,
        items: {
          title: "String",
          type: "string",
          "or:test:value": "test",
        },
      },
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Complex",
    type: "array",
    "or:test:item:count": 2,
    items: {
      title: "Object",
      type: "object",
      "or:test:props": ["string", "array", "boolean"],
      properties: {
        string: {
          title: "String",
          type: "string",
          "or:test:value": "test",
        },
        boolean: {
          title: "Boolean",
          type: "boolean",
          "or:test:value": true,
        },
        array: {
          title: "Array",
          type: "array",
          "or:test:item:count": 2,
          items: {
            title: "String",
            type: "string",
            "or:test:value": "test",
          },
        },
      },
    },
  },
];
