"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.Card elevation={2}>
                <compose.Column padding={16} spacing={8}>
                    <compose.Text key='title' text='Card title' style='titleMedium' />
                    <compose.Text key='body' text='Supporting text inside the card' />
                </compose.Column>
            </compose.Card>
        </compose.Column>
    );
});
