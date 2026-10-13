"ui";

// JSX text children: TypeScript folds white space across lines and decodes entities.

compose.mount(() => {
    return (
        <compose.Column padding={16}>
            <compose.Button onClick={() => {}}>Save</compose.Button>
            <compose.Button onClick={() => {}}>Two words on two lines</compose.Button>
            <compose.Row>
                {'Fish & chips © © A &unknown;'}
            </compose.Row>
            <compose.Row>
                {' padded '}
            </compose.Row>
            <compose.Row>
                {'Count: '}
                {count}
            </compose.Row>
            <compose.Row>
                quoted
                {'double'}
            </compose.Row>
            <compose.Row>
                {'it\'s {braced} <tagged>'}
            </compose.Row>
        </compose.Column>
    );
});
