import { useOf, Source } from '@storybook/blocks';
import ReactMarkdown from 'react-markdown';
import { CopyBlock, atomOneLight } from 'react-code-blocks';

/**
 * A block that displays the story name or title from the of prop
 * - if a story reference is passed, it renders the story name
 * - if a meta reference is passed, it renders the stories' title
 * - if nothing is passed, it defaults to the primary story
 */
export const ReadmeImport = ({ of }) => {
    const resolvedOf = useOf(of || 'story', ['story', 'meta']);
    switch (resolvedOf.type) {
        case 'story': {
            const mdStyle = {
                margin: '-16px 0 32px'
            };
            return <div style={mdStyle}>
                <ReactMarkdown
                    children={resolvedOf.story.parameters.docs.readmeStr || "Could not find README"}
                    style={mdStyle}
                    components={{
                        code(props) {
                            const {children, className, node, ...rest} = props
                            if(className) {
                                const lang = className.replace('language-', '');
                                console.log(lang);
                                return <CopyBlock
                                        text={children}
                                        language={lang}
                                        theme={atomOneLight}
                                        showLineNumbers={false}
                                        wrapLongLines={false}
                                        codeBlock
                                        customStyle={{
                                            borderRadius: '4px',
                                            marginBottom: '8px'
                                        }}
                                    />
                            } else {
                                return <code {...rest} className={className}>{children}</code>
                            }
                        }
                    }}
                ></ReactMarkdown>
            </div>
            /*return <Markdown>{resolvedOf.story.parameters.docs.readmeStr || "Could not find README"}</Markdown>*/
        }
        case 'meta': {
            return <ReactMarkdown>TO BE IMPLEMENTED</ReactMarkdown>;
        }
    }
    return null;
};