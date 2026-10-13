"ui";

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={24}>
            <compose.CircularProgressIndicator key='determinate' progress={0.6} />
            <compose.CircularProgressIndicator key='indeterminate' />
        </compose.Row>
    );
});
