"ui";

let count = compose.state(0);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.Button key='tap' onClick={function () { count.value++; }}>
                {'Clicked ' + count.value + ' times'}
            </compose.Button>
        </compose.Column>
    );
});
