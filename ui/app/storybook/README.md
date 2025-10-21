# Storybook UI for OpenRemote components

For documenting and showcasing UI components, we use a tool called [Storybook](https://storybook.js.org/).<br />
It can also be used as a workshop during development, or to test if components are working as expected.<br />

When running locally, use `npm run serve` in this directory, and navigate to `http://localhost:9000`.<br /> 
If deployed using Docker, you can access it at `https://<your url>/storybook/`.

## Contributing

### "Where are the documentation files?"
Standalone documentation like as "Getting Started" guides, can be found in the `/docs` directory.<br />
It uses simple Markdown ([MDX](https://mdxjs.com)) similar to our [Documentation](https://github.com/openremote/documentation) repository.

Component documentation is located in the individual package directories.<br />
So they can be found at `/ui/component/<package>/stories/*.stories.ts`.<br />
We use the [Component Story Format (CSF)](https://storybook.js.org/docs/api/csf), which is very common across Storybook.<br />
You can check their documentation for more information, or look at existing stories in our repository.

### "How do I add a new component to the documentation?"
Create a `<package>.stories.ts` file in the `/ui/component/<package>/stories` directory.<br />
You can use the [Component Story Format (CSF)](https://storybook.js.org/docs/api/csf), and it will automatically get picked up by Storybook.

If, alongside the playground, you'd like to add text documentation, add an [MDX](https://mdxjs.com) file to the `/docs` directory.<br />
Components can use automatically generated documentation using `<ComponentDocs story={} elementsjson={}>`.<br />
<br />

---

*More documentation to come...*
