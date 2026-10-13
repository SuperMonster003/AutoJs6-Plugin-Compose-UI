"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.Text key='above' text='Above' />
            <compose.HorizontalDivider thickness={1} />
            <compose.Text key='below' text='Below' />
        </compose.Column>
    );
});
