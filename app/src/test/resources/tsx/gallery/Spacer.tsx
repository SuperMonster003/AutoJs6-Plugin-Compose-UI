"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.Text key='above' text='Above the spacer' />
            <compose.Spacer h={24} />
            <compose.Text key='below' text='Below the spacer' />
        </compose.Column>
    );
});
