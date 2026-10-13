"ui";

let cells = [];
for (let i = 0; i < 30; i++) cells.push(compose.Card({key: 'cell-' + i, padding: 16}, compose.Text({text: String(i)})));

compose.mount(function () {
    return (
        <compose.LazyVerticalGrid columns={3} spacing={8} contentPadding={16}>
            {cells}
        </compose.LazyVerticalGrid>
    );
});
