"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={16}>
            <compose.LinearProgressIndicator key='determinate' progress={0.4} />
            <compose.LinearProgressIndicator key='indeterminate' />
        </compose.Column>
    );
});
